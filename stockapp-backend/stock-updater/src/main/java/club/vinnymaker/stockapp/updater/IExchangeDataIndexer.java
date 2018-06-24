package club.vinnymaker.stockapp.updater;

import java.util.Collection;
import java.util.List;

import club.vinnymaker.data.Exchange;
import club.vinnymaker.data.MarketData;

/**
 * An IExchangeDataIndexer object indexes and maintains data of a single stock exchange.
 * 
 * @author evinay
 *
 */
public interface IExchangeDataIndexer {
	/**
	 * Get the exchange indexed by this indexer.
	 * 
	 * @return {@link Exchange}
	 */
	public Exchange getExchange();
	
	/**
	 * Updates the list of stocks maintained by this indexer.
	 * 
	 * @return Updated list of the stocks.
	 */
	List<MarketData> getMarketDataItems();
	
	/**
	 * Synchronizes the recently fetched stock data to the data store.
	 * 
	 * @param exchangeCode Code for this exchange.
	 * @param items Recently fetched stock data.
	 */
	public void syncToDataStore(String exchangeCode, Collection<MarketData> items);
}
