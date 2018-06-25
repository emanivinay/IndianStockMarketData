package club.vinnymaker.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

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

	private static final String INVALID_INDEX_NAME = "";

	private static String getIndexFromStocks(Collection<MarketData> stocks) {
		for (MarketData stock : stocks) {
			if (stock.getType() == MarketDataType.INDEX) {
				return stock.getSymbol();
			}
		}
		return INVALID_INDEX_NAME;
	}

	// CRUD api methods follow.
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
	public boolean updateIndexStocks(String exchangeCode, Collection<MarketData> stocks) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		long exchId = getExchange(exchangeCode, session).getId();

		String index = getIndexFromStocks(stocks);
		Transaction tx = null;
		
		List<MarketData> existing = getAllStocksInAnIndex(index, exchId, session);
		List<Long> newIds = stocks.stream().map(s -> s.getId()).collect(Collectors.toList());
		
		try {
			tx = session.beginTransaction();
			// update the stocks table and the index_listings table.
			tx.commit();
			return true;
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		return false;
	}
	
	private List<MarketData> getAllStocksInAnIndex(String index, long exId, Session session) {
		// TODO(vinay) -> Implement this. 
		Transaction tx = null;
		boolean isTxOpen = false;
		try {
			tx = session.beginTransaction();
			isTxOpen = true;
			
			// First get the stock_index_id with the index name and exchange id.
			String qryStr = String.format("SELECT stock_index_id FROM stock_indexes WHERE exchange_id = %d AND index_name = '%s'",
					exId, index);
			Query<Long> qry = session.createQuery(qryStr, Long.class);
			List<Long> index_ids = qry.list();
			
			if (index_ids.size() != 1) {
				return new ArrayList<>();
			}
			
			long indexId = index_ids.get(0);
			String indexQryStr = String.format("SELECT stock_id FROM index_listings WHERE index_id = %d", indexId);
			List<Long> stock_ids = session.createQuery(indexQryStr, Long.class).list();
			
			// now, get the stocks with these ids.
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> critQry = builder.createQuery(MarketData.class);
			Root<MarketData> root = critQry.from(MarketData.class);
			critQry.select(root).where(builder.in(root.<Long>get("stock_id")).in(stock_ids));
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
