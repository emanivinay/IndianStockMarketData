package club.vinnymaker.stockapp.updater;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import club.vinnymaker.data.Exchange;
import club.vinnymaker.data.MarketData;
import club.vinnymaker.datastore.DataStoreManager;

/**
 * Main executable in the stock updater module. All {@link Exchange} objects are properly
 * initialized and their data is periodically refreshed and synced to the data store.
 * @author evinay
 *
 */
public class Updater {
	
	private static final Logger logger = LogManager.getLogger(Updater.class);

	private static final IExchangeDataIndexer KNOWN_INDEXERS[] = new IExchangeDataIndexer[] {
			NSEDataIndexer.getInstance()
	};
	
	private static final String UPDATER = "updater";
	private static final String CLOSER = "closer";
	
	private static Thread updateThread;
	private static Thread closerThread;
	
	public static void main(String[] args) throws IOException {
		
		logger.info("Entering Updater executable");
	
		updateThread = new Thread(Updater::update);
		updateThread.setName(UPDATER);
		updateThread.start();
		
		closerThread = new Thread(Updater::closer);
		closerThread.setName(CLOSER);
		closerThread.start();
		
		try {
			updateThread.join();
		} catch (InterruptedException e) {
			logger.error("Update thread interrupted - " + e.getMessage());
		}
		
		LogManager.shutdown();
		
		// shutdown the data store manager.
		DataStoreManager.getInstance().shutdown();
	}
	
	private static final long WAIT_TIME_MS = 60000;
	private static final long CLOSE_TIME_MS = 200000;
	
	private static void closer() {
		try {
			Thread.sleep(CLOSE_TIME_MS);
			updateThread.interrupt();
		} catch (InterruptedException e) {
			logger.error("Closer thread interrupted");
		}
	}
	
	private static void update() {
		while (true) {
			for (IExchangeDataIndexer indexer : KNOWN_INDEXERS) {
				if (Thread.interrupted()) {
					logger.info("Update thread interrupted, exiting");
					return;
				}
				
				List<String> indexes = indexer.getExchangeIndexes();
				for (String index : indexes) {
					List<MarketData> items = indexer.getMarketDataItems(index);
					indexer.syncToDataStore(indexer.getExchange().getCode(), items);
				}
			}
			
			try {
				Thread.sleep(WAIT_TIME_MS);
			} catch (InterruptedException e) {
				logger.info("Update thread interrupted, exiting");
				return;
			}
		}
	}
}
