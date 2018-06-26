package club.vinnymaker.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import club.vinnymaker.data.Exchange;
import club.vinnymaker.data.MarketData;
import club.vinnymaker.data.MarketDataLite;
import club.vinnymaker.data.MarketDataType;
import lombok.Getter;

/**
 * A thread safe CRUD layer for stock data in the database.
 * 
 * @author evinay
 *
 */
@Getter
public class StockDataManager {
	private StockDataManager() {
	}

	private static StockDataManager instance;

	static {
		instance = new StockDataManager();
	}

	public static StockDataManager getInstance() {
		return instance;
	}
	
	private static final Logger logger = LogManager.getLogger(StockDataManager.class);
	
	private static MarketData getIndexFromStocks(Collection<MarketData> stocks) {
		for (MarketData stock : stocks) {
			if (stock.getType() == MarketDataType.INDEX) {
				return stock;
			}
		}
		return null;
	}

	// CRUD api methods follow.
	
	private static final String DELETE_INDEX_LISTINGS_QRY_PAT = "DELETE FROM index_listings WHERE index_id = %d AND stock_id IN ";
	private static final String ADD_NEW_INDEX_LISTINGS_QRY_PAT = "INSERT INTO index_listings (index_id, stock_id) VALUES (%d, %d)";
	private static final String GET_STOCK_INDEX_ID_QRY_PAT = "SELECT stock_index_id FROM stock_indexes WHERE index_name = '%s'";
	
	/**
	 * Updates a batch of items(stock/index) from a single index of an exchange in
	 * the database. Any non existing items will be created.
	 * 
	 * @param exchangeCode
	 *            Exchange code.
	 * @param stocks
	 *            Items to be updated.
	 * 
	 * @return True if all the items were successfully updated.
	 */
	@SuppressWarnings("rawtypes")
	public boolean updateIndexStocks(String exchangeCode, Collection<MarketData> stocks) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		long exchId = getExchange(exchangeCode, session).getId();

		MarketData index = getIndexFromStocks(stocks);
		Transaction tx = null;
		List<MarketData> existing = getAllStocksInAnIndex(index.getSymbol(), exchId, session);
		Map<String, MarketData> existingMap = new HashMap<>();
		for (MarketData it : existing) {
			existingMap.put(it.getSymbol(), it);
		}
		
		Set<String> existingIdSet = existing.stream().map((it) -> it.getSymbol()).collect(Collectors.toSet());
		Set<String> newIdSet = stocks.stream().map((it) -> it.getSymbol()).collect(Collectors.toSet());
		String listingsToRemove = getListingsToRemove(newIdSet, existingIdSet);
		
		Date now = new Date();
		
		try {
			tx = session.beginTransaction();
			// First, add new items to the stocks table. We wont be deleting any entries from this table even if
			// they are absent from latest data (possibly cause index has changed, a really rare event).
			boolean hasIndexChanged = false;
			Set<Integer> idsToInsert = new HashSet<>();
			for (MarketData st : stocks) {
				if (!existingIdSet.contains(st.getSymbol())) {
					logger.info("New stock named {} is included in the index.", st.getSymbol());
					session.save(st);
					idsToInsert.add(st.getId());
					hasIndexChanged = true;
				} else {
					// get the persistent version of this item.
					MarketData it = existingMap.get(st.getSymbol());
					it.setLastUpdatedAt(now);
					session.merge(it);
				}
			}
			
			tx.commit();
			tx = session.beginTransaction();
			// Next, if the index has changed, add/remove items from index_listings table.
			if (hasIndexChanged) {
				List l = session.createNativeQuery(String.format(GET_STOCK_INDEX_ID_QRY_PAT, index.getSymbol())).list();
				if (l.size() != 1) {
					tx.commit();
					return false;
				}
				
				int indexId = (Integer) l.get(0);
				int numDeleted = session.createNativeQuery(String.format(DELETE_INDEX_LISTINGS_QRY_PAT, indexId) + listingsToRemove).executeUpdate();
				if (numDeleted > 0) {
					logger.info("{} index listings have been removed", numDeleted);
				}
				
				// Add new items in the data to the index_listings table.
				for (Integer id : idsToInsert) {
					session.createNativeQuery(String.format(ADD_NEW_INDEX_LISTINGS_QRY_PAT, indexId, id)).executeUpdate();
				}
			}
			
			tx.commit();
			return true;
		} catch (HibernateException e) {
			logger.debug("Error updating index stocks - " + e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		return false;
	}
	
	/**
	 * Constructs a string of items to be deleted, to be used in SQL delete query where clauses.
	 * E.g., "(13, 34, 89)" (quotes for clarity).   
	 */
	private String getListingsToRemove(Set<String> newSet, Set<String> existingSet) {
		StringBuffer buffer = new StringBuffer("(");
		for (String e : existingSet) {
			if (!newSet.contains(e)) {
				buffer.append(e + ",");
			}
		}
		
		// Include some impossible id as well so that the string is well formed.
		buffer.append("-1)");
		return buffer.toString();
	}
	
	@SuppressWarnings("rawtypes")
	private List<MarketData> getAllStocksInAnIndex(String index, long exId, Session session) {
		Transaction tx = null;
		boolean isTxOpen = false;
		try {
			tx = session.beginTransaction();
			isTxOpen = true;
			
			// First get the stock_index_id with the index name and exchange id.
			String qryStr = String.format("SELECT stock_index_id FROM stock_indexes WHERE exchange_id = %d AND index_name = '%s'",
					exId, index);
			
			List index_ids = session.createNativeQuery(qryStr).list();
			
			if (index_ids.size() != 1) {
				return new ArrayList<>();
			}
			
			int indexId = (Integer) index_ids.get(0);
			String indexQryStr = String.format("SELECT stock_id FROM index_listings WHERE index_id = %d", indexId);
			List stock_ids = session.createNativeQuery(indexQryStr).list();
			if (stock_ids.isEmpty()) {
				return new ArrayList<>();
			}
			
			// now, get the stocks with these ids.
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> critQry = builder.createQuery(MarketData.class);
			Root<MarketData> root = critQry.from(MarketData.class);
			critQry.select(root).where(root.<Integer>get("id").in(stock_ids));
			return session.createQuery(critQry).list();
		} catch (HibernateException e) {
			if (isTxOpen && tx != null) {
				isTxOpen = false;
				tx.rollback();
			}
		} finally {
			if (isTxOpen && tx != null) {
				isTxOpen = true;
				tx.commit();
			}
		}
		
		return new ArrayList<>();
	}
	
	/**
	 * Retrieves the exchange with the given code from database.
	 *   
	 * @param exCode Exchange code.
	 * @param session An already active hibernate session
	 * 
	 * @return Exchange object retrieved from the database.
	 */
	public Exchange getExchange(String exCode, Session session) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Exchange> query = builder.createQuery(Exchange.class);
			Root<Exchange> root = query.from(Exchange.class);
			query.select(root).where(builder.equal(root.<String>get("code"), exCode));
			List<Exchange> exchanges = session.createQuery(query).list();
			tx.commit();
			
			if (exchanges.size() != 1) {
				return null;
			}
			return exchanges.get(0);
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
		}
		
		return null;
	}
	
	/**
	 * Fetches the Exchange given its code.
	 * 
	 * @param exCode Exchange code
	 * 
	 * @return Exchange object with data fetched from database.
	 */
	public Exchange getExchange(String exCode) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Exchange ex = getExchange(exCode, session);
		session.close();
		return ex;
	}
	
	// APIs for client requests.
	/**
	 * Given an index, retrieves data all its constituent stocks 
	 * @param indexName
	 * @return
	 */
	public Collection<MarketData> getAllMembersData(String indexName) {
		// TODO(vinay) -> Implement this.
		return null;
	}
	
	public Collection<MarketData> getStockData(String symbol) {
		// TODO(vinay) -> Implement this.
		return null;
	}
	
	/**
	 * Returns a list of all matching items, given only part of the name(e.g., stock symbol)
	 *   
	 * @param nameSubStr Part of an item name (stock, index)
	 * 
	 * @return List of all items with a matching name. 
	 */
	public Collection<MarketDataLite> getSearchMatches(String nameSubStr) {
		// TODO(vinay) -> Implement this.
		return null;
	}
}
