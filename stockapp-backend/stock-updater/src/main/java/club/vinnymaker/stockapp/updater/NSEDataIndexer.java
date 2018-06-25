package club.vinnymaker.stockapp.updater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import club.vinnymaker.data.Exchange;
import club.vinnymaker.data.MarketData;
import club.vinnymaker.data.MarketDataType;
import club.vinnymaker.datastore.StockDataManager;

/**
 * NSEDataIndexer maintains the data for NSE. For this indexer, we rely on the live watch page published 
 * by NSE. 
 * 
 * @author evinay
 *
 */
public class NSEDataIndexer implements IExchangeDataIndexer {
	
	private static final String NSE_LIVE_DATA_URL_PREFIX = "https://www.nseindia.com/live_market/dynaContent/live_watch/stock_watch/";
	private static final String NSE50_SUFFIX = "niftyStockWatch.json";
	private static final String NXT50_SUFFIX = "juniorNiftyStockWatch.json";
	private static final String MIDCAP_SUFFIX = "niftyMidcap50StockWatch.json";
	private static final String DATA_KEY = "data";
	private static final String LATEST_DATA_KEY = "latestData";
	private static final String SYMBOL_KEY = "symbol";
	private static final String OPEN_KEY = "open";
	private static final String INDEX_NAME_KEY = "indexName";
	private static final String INDEX_LTP_KEY = "ltp";
	private static final String STOCK_LTP_KEY = "ltP";
	private static final String INDEX_CHG_KEY = "ch";
	private static final String STOCK_CHG_KEY = "ptsC";
	private static final String INDEX_VOL_KEY = "trdVolumesum";
	private static final String STOCK_VOL_KEY = "trdVol";
	private static final String HIGH_KEY = "high";
	private static final String LOW_KEY = "low";
	
	private static NSEDataIndexer instance = null;
	
	private NSEDataIndexer() {
	}
	
	@Override
	public Exchange getExchange() {
		// TODO(vinay) -> Implement this.
		return null;
	}
	
	public static NSEDataIndexer getInstance() {
		if (instance == null) {
			instance = new NSEDataIndexer();
		}
		
		return instance;
	}
	
	@Override
	public List<MarketData> getMarketDataItems() {
		List<MarketData> items = readItemsFromIndexPage(NSE50_SUFFIX);
		items.addAll(readItemsFromIndexPage(NXT50_SUFFIX));
		items.addAll(readItemsFromIndexPage(MIDCAP_SUFFIX));
		return items;
	}
	
	/**
	 * Fetches up to date data for stocks of the given index.
	 *  
	 * @param suffix Index part of the NSE live watch page json data url.
	 *   
	 * @return List of MarketData objects with up to date data.
	 */
	private List<MarketData> readItemsFromIndexPage(String suffix) {
		String url = NSE_LIVE_DATA_URL_PREFIX + suffix;
		Date now = new Date();
		List<MarketData> items = new ArrayList<>();
		try {
			Content content = Request.Get(url).execute().returnContent();
			JSONObject obj = new JSONObject(content.asString());

			// First, get the index item itself.
			JSONObject latestIndexData = (JSONObject) (((JSONArray)obj.get(LATEST_DATA_KEY)).get(0));
			items.add(getMarketDataFromJson(latestIndexData, now, true));

			// Next, get all the component stocks.
			for (Object elem : (JSONArray) obj.get(DATA_KEY) ) {
				items.add(getMarketDataFromJson((JSONObject) elem, now, false));
			}
		} catch (IOException e) {
			// TODO(vinay) -> Error while retrieving response. Should be logged.
		}
		return items;
	}

	/**
	 * Updates the recently fetched stock values to database.
	 * 
	 * @param exchangeCode Code for this {@link Exchange}.
	 * @param items Stocks whose values are to be updated in db. 
	 */
	@Override
	public void syncToDataStore(String exchangeCode, Collection<MarketData> items) {
		StockDataManager.getInstance().updateIndexStocks(exchangeCode, items);
	}

	private double parseDouble(String number) {
		String n = number.replaceAll(",", "");
		return Double.valueOf(n);
	}

	/**
	 * Constructs a {@link MarketData} object with the given json data.
	 * 
	 * @param obj The json object with the data.
	 * @param now The approx time when this data was fetched.
	 * @param isIndex Whether this in an index.
	 * 
	 * @return MarketData object filled with the given data.
	 */
	private MarketData getMarketDataFromJson(JSONObject obj, Date now, boolean isIndex) {
		MarketData item = new MarketData((String) obj.get(isIndex ? INDEX_NAME_KEY : SYMBOL_KEY));
		item.setType(isIndex ? MarketDataType.INDEX : MarketDataType.STOCK);
		item.setOpen(parseDouble((String) obj.get(OPEN_KEY)));
		item.setLastTradedPrice(parseDouble((String) obj.get(isIndex ? INDEX_LTP_KEY : STOCK_LTP_KEY)));
		item.setVolume(parseDouble((String) obj.get(isIndex ? INDEX_VOL_KEY : STOCK_VOL_KEY)));
		item.setHigh(parseDouble((String) obj.get(HIGH_KEY)));
		item.setLow(parseDouble((String) obj.get(LOW_KEY)));
		item.setPreviousClose(item.getLastTradedPrice() - parseDouble((String) obj.get(isIndex ? INDEX_CHG_KEY : STOCK_CHG_KEY)));
		item.setLastUpdatedAt(now);
		return item;
	}
}
