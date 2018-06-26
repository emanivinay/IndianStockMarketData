package club.vinnymaker.data;

import lombok.Getter;

/**
 * A lightweight version of MarketData, contains only full name, exchange id and type(stock/index) info.
 * Useful for responding to search requests from clients. 
 * 
 * @author evinay
 *
 */
@Getter
public class MarketDataLite {
	
	private final int exchangeId;
	private final String fullName;
	private final MarketDataType type;
	
	public MarketDataLite(int exId, String fullName, MarketDataType type) {
		this.exchangeId = exId;
		this.fullName = fullName;
		this.type = type;
	}
}
