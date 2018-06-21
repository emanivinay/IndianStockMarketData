package club.vinnymaker.datastore;

import lombok.Getter;

/**
 * A thread safe CRUD layer for stock data in the database. 
 * 
 * @author evinay
 *
 */
@Getter
public class StockDataManager {
	private StockDataManager() {
	}
	
	private static StockDataManager instance;
	
	static {
		instance = new StockDataManager();
	}
	
	public static StockDataManager getInstance() {
		return instance;
	}
	
	// CRUD api methods follow. 
}
