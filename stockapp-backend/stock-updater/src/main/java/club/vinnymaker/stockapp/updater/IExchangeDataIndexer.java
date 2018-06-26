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
	 * Get the list of indexes managed by this indexer.
	 * 
	 * @return The list of names of indexes managed by this indexer.
	 */
	public List<String> getExchangeIndexes();
	
	/**
	 * Updates the list of stocks in this index, by fetching latest data from the source.  
	 * 
	 * @return Updated list of the stocks.
	 */
	List<MarketData> getMarketDataItems(String index);
	
	/**
	 * Synchronizes the recently fetched stock data(of a single index) to the data store.
	 * 
	 * @param exchangeCode Code for this exchange.
	 * @param items Recently fetched stock data.
	 */
	public void syncToDataStore(String exchangeCode, Collection<MarketData> items);
}
