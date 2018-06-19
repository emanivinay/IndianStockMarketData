package club.vinnymaker.stockapp.updater;

/**
 * A single Stock object represents information about a single share. 
 * 
 * @author evinay
 *
 */
class Stock extends MarketData {
	// open, close(ltp during session), high, low, volume, change(ltp - prevClose)
	
	public Stock(String symbol) {
		super(symbol);
	}

	@Override
	public MarketDataType getType() {
		return MarketDataType.STOCK;
	}
}
