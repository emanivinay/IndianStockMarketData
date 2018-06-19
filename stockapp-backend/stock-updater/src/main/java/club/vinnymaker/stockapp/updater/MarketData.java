package club.vinnymaker.stockapp.updater;

import lombok.Getter;

/**
 * MarketData object represents latest information about a single stock/index. This includes open, close, 
 * high, low, volume traded in the day etc... 
 * 
 * @author evinay
 *
 */
@Getter
public abstract class MarketData {
	// Symbol of this item.
	private final String symbol;
	
	// Opening price.
	private double open;
	
	// Volume traded so far in the day, in millions.
	private double volume;
	
	// Latest traded price.
	private double lastTradedPrice;
	
	// Change from previous closing price in absolute.
	private double change;
	
	protected MarketData(String sym) {
		symbol = sym;
	}
	
	/**
	 * @return Type of this item - stock or index etc...
	 */
	abstract public MarketDataType getType();
}
