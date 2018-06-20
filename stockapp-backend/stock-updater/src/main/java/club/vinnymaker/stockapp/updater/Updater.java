package club.vinnymaker.stockapp.updater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Main executable in the stock updater module. All {@link Exchange} objects are properly
 * initialized and their data is periodically refreshed and synced to the data store using
 * a thread pool.
 * 
 * @author evinay
 *
 */
public class Updater {

	private static final String EXCHANGE_FILE = "exchanges.txt";
	
	public static void main(String[] args) {
		Updater updater = new Updater();
		updater.init();
	}
	
	private List<Exchange> exchanges;

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
					System.out.println(ex.getTitle());
					exchanges.add(ex);
				}
			}
			
			reader.close();
		} catch (IOException e) {
			// TODO(vinay) -> Unable to read the properties file on runtime classpath. Log this.
		}
	}
	
	private void shutdown() {
	}
}
