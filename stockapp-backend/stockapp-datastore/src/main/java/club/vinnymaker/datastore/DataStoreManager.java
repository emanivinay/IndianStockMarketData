package club.vinnymaker.datastore;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import lombok.Getter;

@Getter
public class DataStoreManager {
	private static DataStoreManager dataStoreMgr = null;
	private SessionFactory factory = null;
	
	private DataStoreManager() {
		init();
	}
	
	private void init() {
		try {
			factory = new Configuration().configure().buildSessionFactory();
		} catch (HibernateException e) {
		}
	}
	
	public void shutdown() {
		if (factory != null) {
			factory.close();
		}
	}
	
	public static DataStoreManager getInstance() {
		if (dataStoreMgr == null) {
			dataStoreMgr = new DataStoreManager();
		}
		return dataStoreMgr;
	}
}
