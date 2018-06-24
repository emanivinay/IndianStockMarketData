package club.vinnymaker.data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

/**
 * MarketData object represents latest information about a single stock/index. This includes open, close, 
 * high, low, volume traded in the day etc... 
 * 
 * @author evinay
 *
 */
@Entity(name = "stocks")
@Getter
@Setter
public class MarketData {
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="stock_id_gen")
	@SequenceGenerator(name = "stock_id_gen", sequenceName = "stocks_stock_id_seq", allocationSize = 1)
	@Column(name = "stock_id")
	private long id;
	
	// Symbol of this item.
	@Column(name = "symbol")
	private final String symbol;
	
	// Opening price.
	@Column(name = "open")
	private double open;
	
	// Volume traded so far in the day, in millions.
	@Column(name = "volume")
	private double volume;
	
	// Latest traded price.
	@Column(name = "ltp")
	private double lastTradedPrice;
	
	// Previous closing price.
	@Column(name = "prev_close")
	private double previousClose;
	
	// Highest of the day.
	@Column(name = "high")
	private double high;
	
	// Lowest of the day.
	@Column(name = "low")
	private double low;
	
	// Type of the item - stock or index.
	private MarketDataType type;
	
	// Time of the latest update for this item.
	private Date lastUpdatedAt;
	
	// Id of the exchange this item belongs to.
	@Column(name = "exchange_id")
	private long exchangeId;
	
	public MarketData(String sym) {
		symbol = sym;
	}
	
	public MarketData() {
		this(null);
	}
	
	/**
	 * @return Change in value from previous day's close.
	 */
	public double getChange() {
		return lastTradedPrice - previousClose;
	}
	
	/** Pattern string for displaying MarketData objects. */
	private static final String REPR_PAT = "%s(open=%f, lastTradedPrice=%f, vol=%f, high=%f, low=%f, prevClose=%f, lastUpdatedAt=%s)";
	
	@Override
	public String toString() {
		// Stock(open=o, ltp=l, vol=vol, high=h, low=lo, prevClose=prev, last=last);
		return String.format(REPR_PAT, type.toString(), open, lastTradedPrice, volume, high, low, previousClose, lastUpdatedAt);
	}
}
