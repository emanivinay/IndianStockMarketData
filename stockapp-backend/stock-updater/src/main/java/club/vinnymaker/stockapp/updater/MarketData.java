package club.vinnymaker.stockapp.updater;

import java.util.Date;

import lombok.Getter;

/**
 * MarketData object represents latest information about a single stock/index. This includes open, close, 
 * high, low, volume traded in the day etc... 
 * 
 * @author evinay
 *
 */
@Getter
public class MarketData {
	// Symbol of this item.
	private final String symbol;
	
	// Opening price.
	private double open;
	
	// Volume traded so far in the day, in millions.
	private double volume;
	
	// Latest traded price.
	private double lastTradedPrice;
	
	// Previous closing price.
	private double previousClose;
	
	// Type of the item - stock or index.
	private MarketDataType type;
	
	// Time of the latest update for this item.
	private Date lastUpdatedAt;
	
	protected MarketData(String sym) {
		symbol = sym;
	}
	
	/**
	 * @return Change in value from previous day's close.
	 */
	public double getChange() {
		return lastTradedPrice - previousClose;
	}
}
