package club.vinnymaker.datastore;

import java.util.ArrayList;
import java.util.Collection;
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
					// get the persistent version of this item and update it's contents with recent data.
					MarketData it = existingMap.get(st.getSymbol());
					st.update(it);
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
	private static final String GET_INDEX_ID_QRY_PAT = "SELECT stock_index_id FROM stock_indexes WHERE index_name = '%s' AND exchange_id = %d";
	private static final String GET_INDEX_STOCK_IDS_QRY_PAT = "SELECT stock_id FROM index_listings WHERE index_id = %d";
	
	/**
	 * Retrieves the latest data of a single symbol(repr. stock/index) on an exchange.
	 *  
	 * @param exchange Code of the exchange.
	 * @param symbol Stock/index symbol.
	 * 
	 * @return Latest data of the symbol.
	 */
	public MarketData getStockData(String exchange, String symbol) {
		if (exchange == null || symbol == null) {
			return null;
		}
		
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Exchange ex = getExchange(exchange, session);
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> qry = builder.createQuery(MarketData.class);
			Root<MarketData> root = qry.from(MarketData.class);
			qry.select(root).where(builder.equal(root.<Integer>get("exchangeId"), ex.getId()),
								  builder.equal(root.<Integer>get("symbol"), symbol));
			List<MarketData> ret = session.createQuery(qry).list();
			tx.commit();
			if (ret.size() != 1) {
				logger.debug("{} item(s) found with symbol {} on exchange {}", ret.size(), symbol, ex.getCode());
				return null;
			}
			return ret.get(0);
		} catch (HibernateException e) {
			logger.debug("Error querying for MarketData " + e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		return null;
	}
	
	/**
	 * Retrieves data of the constituent stocks on an index.
	 *  
	 * @param exCode Exchange code.
	 * @param indexName Name of the index.
	 * 
	 * @return The given index's constituent stock data.(including the index itself)
	 */
	@SuppressWarnings("rawtypes")
	public Collection<MarketData> getAllMembersData(String exCode, String indexName) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Exchange ex = getExchange(exCode, session);
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			// SELECT stock_index_id FROM stock_indexes WHERE index_name = indexName AND exchange_id = exId;
			List idList = session.createNativeQuery(String.format(GET_INDEX_ID_QRY_PAT, indexName, ex.getId())).list();
			if (idList.size() != 1) {
				// Can't have more than 1 index with the same name.
				tx.commit();
				return null;
			}
			
			int indexId = (Integer) idList.get(0);
			// SELECT stock_id FROM index_listings WHERE index_id = indexId;
			List stockIdList = session.createNativeQuery(String.format(GET_INDEX_STOCK_IDS_QRY_PAT, indexId)).list();
			
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> critQry = builder.createQuery(MarketData.class);
			Root<MarketData> root = critQry.from(MarketData.class);
			critQry.select(root).where(root.<Integer>get("id").in(stockIdList));
			List<MarketData> ret = session.createQuery(critQry).list();
			tx.commit();
			return ret;
		} catch (HibernateException e) {
			logger.info("Error retrieving index item with the name " + indexName + " on exchange " + exCode);
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		return null;
	}

	/**
	 * Retrieves a list of {@link Exchange} objects given their ids.
	 * 
	 * @param exIds A list of exchange ids whose data is to be fetched.
	 * 
	 * @return A list of {@link Exchange} objects.
	 */
	public List<Exchange> getExchanges(Collection<Integer> exIds) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Exchange> qry = builder.createQuery(Exchange.class);
			Root<Exchange> root = qry.from(Exchange.class);
			qry.select(root).where(root.<Integer>get("id").in(exIds));
			List<Exchange> exchanges = session.createQuery(qry).list();
			tx.commit();
			return exchanges;
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		return new ArrayList<>();
	}
	
	/**
	 * Returns a list of all matching items, given only part of the name(e.g., stock symbol)
	 *   
	 * @param nameSubStr Part of an item name (stock, index)
	 * 
	 * @return List of all items with a matching name. 
	 */
	public Collection<MarketDataLite> getSearchMatches(String nameSubStr) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> qry = builder.createQuery(MarketData.class);
			Root<MarketData> root = qry.from(MarketData.class);
			qry.select(root).where(builder.like(root.<String>get("symbol"), "%" + nameSubStr + "%"));
			List<MarketData> ret = session.createQuery(qry).list();
			tx.commit();
			
			// MarketData items retrieved from db don't have types populated. Should make one more 
			// query to populate type fields for these objects.
			populateTypeFields(ret, session);
			return ret.stream().map((it) -> it.liteWeightVersion()).collect(Collectors.toList());
		} catch (HibernateException e) {
			logger.debug("Error querying for stock items " + e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
			return new ArrayList<>();
		} finally {
			session.close();
		}		
	}
	
	private static final String GET_INDEXES_QRY_PAT = "SELECT exchange_id, index_name FROM stock_indexes";
	private static final String GET_INDEXES_SINGLE_EXCHANGE_QRY_PAT = "SELECT index_name FROM stock_indexes WHERE exchange_id = %d";
	
	/**
	 * Returns all the indexes of an exchange 
	 * 
	 * @param exchangeId Exchange id.
	 * 
	 * @return List of Index objects.
	 */
	@SuppressWarnings("rawtypes")
	public List<MarketData> getIndexes(int exchangeId) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List indexNames = session.createNativeQuery(String.format(GET_INDEXES_SINGLE_EXCHANGE_QRY_PAT, exchangeId)).list();
			
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> query = builder.createQuery(MarketData.class);
			Root<MarketData> root = query.from(MarketData.class);
			query.select(root).where(builder.equal(root.<Integer>get("exchangeId"), exchangeId),
									root.<String>get("symbol").in(indexNames));
			List<MarketData> ret = session.createQuery(query).list();
			tx.commit();
			return ret;
		} catch (HibernateException e) {
			logger.debug("Error querying database for indexes of the exchange with id " + exchangeId);
			logger.debug("Error is " + e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		
		return new ArrayList<>();
	}
	
	/**
	 * Populates type fields for the given {@link MarketData} items.
	 * 
	 * @param items Collection of stocks whose types are to be determined.
	 * @param session Currently active hibernate session, under whose context this routine runs.
	 */
	@SuppressWarnings("rawtypes")
	private void populateTypeFields(Collection<MarketData> items, Session session) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			// There are not many indexes supported, so get them all.
			List idxPairs = session.createNativeQuery(GET_INDEXES_QRY_PAT).list();
			tx.commit();
			
			for (MarketData it : items) {
				Integer exID = it.getExchangeId();
				String sym = it.getSymbol();
				
				boolean isAPairing = false;
				for (Object pair : idxPairs) {
					Object[] comps = (Object[]) pair;
					if (exID.equals(comps[0]) && sym.equals(comps[1])) {
						isAPairing = true;
						break;
					}
				}
				
				it.setType(isAPairing ? MarketDataType.INDEX : MarketDataType.STOCK);
			}
		} catch (HibernateException e) {
			logger.debug("Error querying database for stock types " + e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		}
	}
}
