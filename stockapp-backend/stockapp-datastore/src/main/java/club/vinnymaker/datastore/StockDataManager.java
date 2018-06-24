package club.vinnymaker.datastore;

import java.util.Collection;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;

import club.vinnymaker.data.MarketData;
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
		long exchId = getExchangeIdWithCode(exchangeCode, session);
		if (exchId == INVALID_EXCHANGE_ID) {
			// no exchange with the given code.
			session.close();
			return false;
		}

		String index = getIndexFromStocks(stocks);
		Transaction tx = null;

		try {
			tx = session.beginTransaction();
			// update the stocks table and the index_listings table.

			// First fetch the stocks from database and map them to their ids.
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<MarketData> qry = builder.createQuery(MarketData.class);
			Root<MarketData> root = qry.from(MarketData.class);
			qry.select(root).where(builder.equal(root.<Long>get("exchange_id"), exchId));
			tx.commit();
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			session.close();
		}
		return false;
	}

	// An impossible id to denote an invalid exchange.
	private static final long INVALID_EXCHANGE_ID = -1L;

	// SQL query for fetching exchange id from its name.
	private static final String GET_EXCHANGE_ID_QRY_STR = "SELECT exchange_id from exchanges WHERE code = '%s'";

	/**
	 * Exchanges are referred to in our code using exchange codes(e.g., NSE). During
	 * database operations however, we use integer ids. This function returns the id
	 * of the exchange with the given code. It does its querying in the context of
	 * an already active hibernate session, without creating a new one. So callers
	 * must create a new session and pass it.
	 */
	private long getExchangeIdWithCode(String exchangeCode, Session session) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			NativeQuery<Long> query = session.createNativeQuery(String.format(GET_EXCHANGE_ID_QRY_STR, exchangeCode),
					Long.class);
			Long ret = query.getSingleResult();
			tx.commit();
			return ret;
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
		}

		return INVALID_EXCHANGE_ID;
	}
}
