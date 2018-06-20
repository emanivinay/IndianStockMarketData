package club.vinnymaker.stockapp.updater;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import club.vinnymaker.data.MarketData;
import lombok.Getter;

/**
 * Represents a single exchange, e.g., NSE, BSE etc...
 * @author evinay
 *
 */
@Getter
public class Exchange {
	
	// Indexer object for this exchange.
	private final IExchangeDataIndexer indexer;
	
	// Time when this exchange's data was last updated.
	private Date lastUpdatedAt;
	
	private Exchange(String title, IExchangeDataIndexer indexer) {
		this.title = title;
		items = new HashMap<>();
		this.indexer = indexer;
		lastUpdatedAt = null;
	}
	
	// A threshold of 1 minute is applied before refreshing the item data.
	public static final long REFRESH_THRESHOLD_MS = 60000; 
			
	// A predefined list of stock exchanges.
	private static final Map<String, Exchange> knownExchanges;
	
	static {
		knownExchanges = new HashMap<>();
		knownExchanges.put("NSE", new Exchange("National Stock Exchange of India", NSEDataIndexer.getInstance()));
		
		// Add more as they're supported.
	}
	
	// Title of this exchange, e.g., National Stock Exchange of India
	private final String title;
	
	// List of stocks traded on this exchange. This list may not include all of the stocks 
	// traded there, but only those part of popular indices.
	private final Map<String, MarketData> items;
	
	/**
	 * Gets the Exchange with the given code.
	 * 
	 * @param exchgCode Exchange code, e.g., NSE.
	 * @return Exchange with the given code if it exists, null otherwise.
	 */
	public static Exchange getExchange(String exchgCode) {
		return knownExchanges.get(exchgCode);
	}
	
	/**
	 * Update stocks maintained in this exchange. 
	 */
	public void updateItems() {
		Date now = new Date();
		if (lastUpdatedAt == null || now.getTime() - lastUpdatedAt.getTime() >= REFRESH_THRESHOLD_MS) {
			indexer.getMarketDataItems().stream().forEach((item) -> items.put(item.getSymbol(), item));
			lastUpdatedAt = now;
			indexer.syncToDataStore(items.values());
		}
	}
}
