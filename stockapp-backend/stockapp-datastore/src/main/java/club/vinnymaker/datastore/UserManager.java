package club.vinnymaker.datastore;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import club.vinnymaker.data.User;

public class UserManager {
	// loadUser, createUser, updateUser, deleteUser.
	private static UserManager userMgr = null;
	private UserManager() {
	}
	
	public static UserManager getInstance() {
		if (userMgr == null) {
			userMgr = new UserManager();
		}
		return userMgr;
	}
	
	public Long createUser(User newUser) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		Long newUserId = null;
		
		try {
			tx = session.beginTransaction();
			newUserId = (Long) session.save(newUser);
			tx.commit();
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
			newUserId = null;
		} finally {
			session.close();
		}
		
		return newUserId;
	}
}
