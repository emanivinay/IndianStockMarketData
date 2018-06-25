package club.vinnymaker.stockapp.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String line = reader.readLine().trim();
			if (line.equals("END")) {
				break;
			}
			
			Exchange ex = StockDataManager.getInstance().getExchange(line);
			String message = ex == null ? "No exchange exists in database with the code" : "Exchange id is " + ex.getId();
			System.out.println(message);
		}
		
		System.out.println("Exited main loop");
		reader.close();
		
		// shutdown the data store manager.
		DataStoreManager.getInstance().shutdown();
	}
}
