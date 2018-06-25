package club.vinnymaker.stockapp.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import club.vinnymaker.data.Exchange;
import club.vinnymaker.datastore.DataStoreManager;
import club.vinnymaker.datastore.StockDataManager;

/**
 * Main executable in the stock updater module. All {@link Exchange} objects are properly
 * initialized and their data is periodically refreshed and synced to the data store.
 * @author evinay
 *
 */
public class Updater {
	
	private static final Logger logger = LogManager.getLogger(Updater.class);
	
	public static void main(String[] args) throws IOException {
		
		logger.info("Entering Updater executable");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String line = reader.readLine().trim();
			if (line.equalsIgnoreCase("END")) {
				break;
			}
			
			Exchange ex = StockDataManager.getInstance().getExchange(line.toUpperCase());
			String message = ex == null ? "No exchange exists in database with the code" : "Exchange id is " + ex.getId();
			System.out.println(message);
		}
		
		System.out.println("Exited main loop");
		reader.close();
		
		LogManager.shutdown();
		
		// shutdown the data store manager.
		DataStoreManager.getInstance().shutdown();
	}
}
