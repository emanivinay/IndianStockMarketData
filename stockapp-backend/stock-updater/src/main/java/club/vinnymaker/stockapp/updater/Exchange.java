package club.vinnymaker.stockapp.updater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * Represents a single exchange, e.g., NSE, BSE etc...
 * @author evinay
 *
 */
@Getter
public class Exchange {
	
	private Exchange(String title) {
		this.title = title;
		stocks = new HashMap<>();
	}
	
	// A predefined list of stock exchanges.
	private static final Map<String, Exchange> knownExchanges;
	
	static {
		knownExchanges = new HashMap<>();
		knownExchanges.put("NSE", new Exchange("National Stock Exchange of India"));
		
		// Add more as they're supported.
	}
	
	// Title of this exchange, e.g., National Stock Exchange of India
	private final String title;
	
	// List of stocks traded on this exchange. This list may be incomplete as 
	// only those stocks in popular indices may be maintained. 
	private final Map<String, Stock> stocks;
	
	/**
	 * Gets the Exchange with the given code.
	 * 
	 * @param exchgCode Exchange code, e.g., NSE.
	 * @return Exchange with the given code if it exists, null otherwise.
	 */
	public static Exchange getExchange(String exchgCode) {
		return knownExchanges.get(exchgCode);
	}
	
	@SuppressWarnings("unused")
	private List<Stock> getStocks() {
		return null;
	}
	
	/**
	 * Get the stock traded by this symbol in this exchange.
	 *  
	 * @param stockCode Stock symbol.
	 * @return Stock object with this symbol.
	 */
	public Stock getStock(String stockCode) {
		return stocks.get(stockCode);
	}
	
	@SuppressWarnings("unused")
	private void populate() {
	}
}
