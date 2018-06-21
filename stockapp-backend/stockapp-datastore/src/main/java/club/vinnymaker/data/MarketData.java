package club.vinnymaker.data;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * MarketData object represents latest information about a single stock/index. This includes open, close, 
 * high, low, volume traded in the day etc... 
 * 
 * @author evinay
 *
 */
@Getter
@Setter
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
	
	// Highest of the day.
	private double high;
	
	// Lowest of the day.
	private double low;
	
	// Type of the item - stock or index.
	private MarketDataType type;
	
	// Time of the latest update for this item.
	private Date lastUpdatedAt;
	
	public MarketData(String sym) {
		symbol = sym;
	}
	
	/**
	 * @return Change in value from previous day's close.
	 */
	public double getChange() {
		return lastTradedPrice - previousClose;
	}
	
	private static final String REPR_PAT = "%s(open=%f, lastTradedPrice=%f, vol=%f, high=%f, low=%f, prevClose=%f, lastUpdatedAt=%s)";
	
	@Override
	public String toString() {
		// Stock(open=o, ltp=l, vol=vol, high=h, low=lo, prevClose=prev, last=last);
		return String.format(REPR_PAT, type.toString(), open, lastTradedPrice, volume, high, low, previousClose, lastUpdatedAt);
	}
}
