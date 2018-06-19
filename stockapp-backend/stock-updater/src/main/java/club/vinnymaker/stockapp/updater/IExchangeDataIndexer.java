package club.vinnymaker.stockapp.updater;

import java.util.List;

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
	 * The list of indices/stocks maintained by this indexer. 
	 */
	List<MarketData> getMarketDataItems();
}
