package club.vinnymaker.stockapp.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


import static club.vinnymaker.stockapp.updater.Exchange.REFRESH_THRESHOLD_MS;

/**
 * Main executable in the stock updater module. All {@link Exchange} objects are properly
 * initialized and their data is periodically refreshed and synced to the data store using
 * a thread pool.
 * 
 * @author evinay
 *
 */
public class Updater {

	// Resource file named 'exchanges.txt' should be present on the class path. 
	private static final String EXCHANGE_FILE = "exchanges.txt";
	
	public static void main(String[] args) {
		Updater updater = new Updater();
		updater.init();
	}
	
	private List<Exchange> exchanges;
	private Thread updateThread;

	private Updater() {
		exchanges = new ArrayList<>();
	}
	
	private void init() {
		try {
			InputStream istream = getClass().getClassLoader().getResourceAsStream(EXCHANGE_FILE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
			String line;
			while ((line = reader.readLine()) != null) {
				Exchange ex = Exchange.getExchange(line.trim());
				if (ex != null) {
					exchanges.add(ex);
				}
			}
			
			reader.close();
		} catch (IOException e) {
			// TODO(vinay) -> Unable to read the properties file on runtime classpath. Log this.
		}
		
		// Initialize 1 or more threads for updating exchange data.
		updateThread = new Thread(this::updateExchanges);
		updateThread.start();
		
		// Wait for the update thread to shut down.
		try {
			updateThread.join();
		} catch (InterruptedException e) {
			// This is not possible, but exit here.
		}
	}
	
	private void updateExchanges() {
		try {
			while (true) {
				if (Thread.interrupted()) {
					break;
				}
				
				// Wait a minute before refreshing data.
				Thread.sleep(REFRESH_THRESHOLD_MS);
				
				for (Exchange ex : exchanges) {
					if (Thread.interrupted()) {
						break;
					}
					
					// Refresh this exchange's data and sync to datastore.
					ex.updateItems();
				}
			}
		} catch (InterruptedException e) {
			// this thread is interrupted, which means shutdown() is called, stop here.
			// TODO(vinay) -> This must be logged.
		}
	}
	
	@SuppressWarnings("unused")
	private void shutdown() {
		// Interrupt the update thread, which will wake up
		updateThread.interrupt();
	}
}
