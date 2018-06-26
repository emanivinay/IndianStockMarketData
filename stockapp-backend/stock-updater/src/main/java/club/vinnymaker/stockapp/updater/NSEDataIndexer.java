package club.vinnymaker.stockapp.updater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	
	private static final String EXCHANGE_CODE_NSE = "NSE";
	
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
	
	private static final Logger logger = LogManager.getLogger(NSEDataIndexer.class); 
	
	private NSEDataIndexer() {
	}
	
	@Override
	public Exchange getExchange() {
		return StockDataManager.getInstance().getExchange(EXCHANGE_CODE_NSE);
	}
	
	public static NSEDataIndexer getInstance() {
		if (instance == null) {
			instance = new NSEDataIndexer();
		}
		
		return instance;
	}
	
	/**
	 *  Names of indexes maintained by this indexer. These should match the names stored in db as well as
	 *  the names from the data source. 
	 */
	private static final String INDEX_NSE50 = "NIFTY 50";
	private static final String INDEX_NXT50 = "NIFTY NEXT 50";
	private static final String INDEX_MIDCAP = "NIFTY MIDCAP 50";
	private static final String[] INDEXES_LIST = new String[] {INDEX_NSE50, INDEX_NXT50, INDEX_MIDCAP};
	
	private static final Map<String, String> INDEX_NAME_SUFFIX_MAP = new HashMap<>();
	
	static {
		INDEX_NAME_SUFFIX_MAP.put(INDEX_NSE50, NSE50_SUFFIX);
		INDEX_NAME_SUFFIX_MAP.put(INDEX_NXT50, NXT50_SUFFIX);
		INDEX_NAME_SUFFIX_MAP.put(INDEX_MIDCAP, MIDCAP_SUFFIX);
	}
	
	@Override
	public List<String> getExchangeIndexes() {
		return Arrays.asList(INDEXES_LIST);
	}
	
	@Override
	public List<MarketData> getMarketDataItems(String index) {
		String suffix = INDEX_NAME_SUFFIX_MAP.get(index);
		return readItemsFromIndexPage(suffix);
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
			// Insert into latestIndexData a few keys that're present in the outer object.
			latestIndexData.put(INDEX_VOL_KEY, obj.get(INDEX_VOL_KEY));
			items.add(getMarketDataFromJson(latestIndexData, now, true));

			// Next, get all the component stocks.
			for (Object elem : (JSONArray) obj.get(DATA_KEY) ) {
				items.add(getMarketDataFromJson((JSONObject) elem, now, false));
			}
		} catch (IOException e) {
			logger.debug("Error retrieving items from data source " + e.getMessage());
		}
		return items;
	}

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
		item.setExchangeId(getExchange().getId());
		return item;
	}
}
