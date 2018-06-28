package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import club.vinnymaker.data.MarketData;
import club.vinnymaker.data.MarketDataLite;
import club.vinnymaker.datastore.StockDataManager;

/**
 * StockController processes stock data requests from clients. All stock controller requests must
 * authenticate by passing Authorization header as well as a username header in their requests.
 *  
 * @author evinay
 *
 */
public class StockController extends BaseController {
	
	private static final String USERNAME_HEADER = "Username";
	private static final String EXCHANGE_PARAM = "exchange";
	private static final String SYMBOL_PARAM = "symbol";
	private static final String NAME_SUBSTR_KEY = "substr";
	private static final String RESULTS_KEY = "results";
	private static final String EXCHANGE_IDS_KEY = "exids";
	private static final String EXCHANGE_ID_KEY = "exid";
	
	private static final int MIN_SEARCH_KEY_SIZE = 2;
	
	/**
	 * Authenticate the request using username request header.
	 * 
	 * @param req Http request.
	 * @return True if successfully authenticated, false otherwise.
	 */
	private static boolean authenticate(HttpServletRequest req) {
		String unameInHeader = req.getHeader(USERNAME_HEADER);
		if (unameInHeader == null) {
			return false;
		}
		
		return authenticate(req, unameInHeader);
	}
	
	private static <T> JSONObject stuffArrayIntoObject(String key, Collection<T> array) {
		JSONObject ret = new JSONObject();
		ret.put(key, array);
		return ret;
	}
	
	/**
	 * Returns data for a single item(share/index) given its symbol and exchange.
	 * 
	 * @param req Http request
	 * @param resp Response to be sent back to client.
	 * @param namedParams Any named parameters in the uri.
	 * 
	 */
	public static void getItemData(HttpServletRequest req, HttpServletResponse resp, 
			Map<String, String> namedParams) throws IOException {
		if (!authenticate(req)) {
			authFailure(resp);
			return;
		}
		
		String exchange = namedParams.get(EXCHANGE_PARAM);
		String symbol = namedParams.get(SYMBOL_PARAM);
		
		MarketData itemData = StockDataManager.getInstance().getStockData(exchange, symbol);
		if (itemData == null) {
			// Requested item not found.
			error(resp, HttpServletResponse.SC_NOT_FOUND, RESOURCE_DOESNT_EXIST_ERROR);
			return;
		}
		
		success(resp, new JSONObject(itemData));
	}
	
	/**
	 * Returns the list of all components(names, type) of an index.
	 * 
	 * @param req HTTP request
	 * @param resp Response to be constructed and sent back.
	 * @param named Named params in the uri.
	 */
	public static void getIndexComponents(HttpServletRequest req, HttpServletResponse resp, Map<String, String> named)
			throws IOException {
		if (!authenticate(req)) {
			authFailure(resp);
			return;
		}
		
		String exCode = named.get(EXCHANGE_PARAM);
		String indexName = named.get(SYMBOL_PARAM);
		Collection<MarketData> items = StockDataManager.getInstance().getAllMembersData(exCode, indexName);
		if (items == null) {
			error(resp, HttpServletResponse.SC_NOT_FOUND, "Requested index not found on the exchange.");
			return;
		}
		
		success(resp, stuffArrayIntoObject("items", items));
	}
	
	/**
	 * Given a partial string, returns a list of all stocks/indexes with matching names.
	 *  
	 * @param req Http request.
	 * @param resp Response object to be constructed.
	 * @param named Any named params in the uri.
	 */
	public static void getMatches(HttpServletRequest req, HttpServletResponse resp, Map<String, String> named)
		throws IOException {
		if (!authenticate(req)) {
			authFailure(resp);
			return;
		}
		
		String substr = named.get(NAME_SUBSTR_KEY);
		JSONObject obj = new JSONObject();
		if (substr == null || substr.length() < MIN_SEARCH_KEY_SIZE) {
			// Empty string, return an empty result.
			obj.put(RESULTS_KEY, new ArrayList<>());
		} else {
			Collection<MarketDataLite> results = StockDataManager.getInstance().getSearchMatches(substr.toUpperCase());
			obj.put(RESULTS_KEY, results);
		}
		success(resp, obj);
	}
	
	/**
	 * Returns all the indexes for the given exchange.
	 * 
	 * @param req
	 * @param resp
	 * @param named
	 * 
	 * @throws IOException
	 */
	public static void getIndexes(HttpServletRequest req, HttpServletResponse resp, Map<String, String> named)
		throws IOException {
		if (!authenticate(req)) {
			authFailure(resp);
			return;
		}
		
		int exId = Integer.parseInt(named.get(EXCHANGE_ID_KEY));
		success(resp, stuffArrayIntoObject("indexes", StockDataManager.getInstance().getIndexes(exId)));
	}
	
	/**
	 * Returns information about one or more exchanges.
	 * 
	 * @param req
	 * @param resp
	 * @param named
	 * 
	 * @throws IOException
	 */
	public static void getExchanges(HttpServletRequest req, HttpServletResponse resp, Map<String, String> named)
		throws IOException {
		if (!authenticate(req)) {
			authFailure(resp);
			return;
		}
		
		List<Integer> exIds = processExchangeIdParts(named.get(EXCHANGE_IDS_KEY));
		if (exIds == null) {
			error(resp, HttpServletResponse.SC_BAD_REQUEST, "Bad request");
			return;
		}
		
		success(resp, stuffArrayIntoObject("exchanges", StockDataManager.getInstance().getExchanges(exIds)));
	}
	
	private static List<Integer> processExchangeIdParts(String e) {
		if (e == null || e.isEmpty()) {
			return null;
		}
		
		try {
			return Arrays.asList(e.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList());
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
