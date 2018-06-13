package club.vinnymaker.datastore;

import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import club.vinnymaker.data.User;

public class UserManager {
	
	private static final int USERNAME_LENGTH_MIN = 5;
	private static final int PASSWORD_LENGTH_MIN = 8;
	
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
	
	private String createRandomSalt() {
		// TODO(vinay) -> Implement this.
		return "sat";
	}
	
	private String getPasswordHash(String password, String salt) {
		// TODO(vinay) -> Implement this.
		return "has";
	}
	
	public User loadUser(long userId) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		
		try {
			tx = session.beginTransaction();
			User user = (User) session.get(User.class, userId);
			tx.commit();
			return user;
		} catch(HibernateException e) {
			e.printStackTrace();
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
		
		return null;
	}
	
	/**
	 * Verifies that the user details are valid and persists an updated user in the database.
	 *   
	 * @param user A detached User object whose value has just changed.
	 * @return True if successfully updated, false otherwise.
	 * 
	 * @throws InvalidUserException when the user details are not valid.
	 */
	public boolean updateUser(User user) throws InvalidUserException {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.merge(user);
			tx.commit();
			return true;
		} catch (HibernateException e) {
			// TODO(vinay) -> Error occurred while updating a user, this must be logged.
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return false;
	}
	
	/**
	 * Deletes a user from the database with the given user id.
	 * 
	 * @param userId Id of the user to be deleted.
	 * @return True if user successfully deleted and false otherwise(usually means no such user existed).
	 */
	public boolean deleteUser(Long userId) {
		User user = loadUser(userId);
		if (user == null) {
			return false;
		}
		
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.delete(user);
			tx.commit();
			return true;
		} catch (HibernateException e) {
			// TODO(vinay) -> Error deleting the user, must be logged.
			if (tx != null) {
				tx.rollback();
			}
			return false;
		} finally {
			if (session != null) {
				session.close();
			}
		}		
	}
	
	/**
	 * Creates a new user in the database and returns its id. 
	 * 
	 * @param username Username must be a alphanumeric string, unique among all users. 
	 * @param password Must be alphanumeric, at least 8 characters long.
	 * 
	 * @return The id of the newly created user if successful, null otherwise.
	 * 
	 * @throws InvalidUserException when username or password provided is not valid.
	 */
	public Long createUser(String username, String password) throws InvalidUserException {
		// First validate username and password.
		if (!isValidAlphaInput(username, USERNAME_LENGTH_MIN)) {
			throw new InvalidUserException("Invalid username. Username must be alphanumeric and atleast 5 characters long.");
		}
		if (!isValidAlphaInput(password, PASSWORD_LENGTH_MIN)) {
			throw new InvalidUserException("Invalid password. Password must be at least 8 chars long and alphanumeric");
		}
		
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			User user = new User();
			String salt = createRandomSalt();
			String hash = getPasswordHash(password, salt);
			
			user.setUsername(username);
			user.setPasswordHash(hash);
			user.setPasswordSalt(salt);
			user.setDateCreated(new Date(System.currentTimeMillis()));
			Long id = (Long) session.save(user);
			tx.commit();
			return id;
		} catch (HibernateException e) {
			e.printStackTrace();
			if (tx != null)
				tx.rollback();
		} finally {
			if (session != null) {
				session.close();
			}
		}
		return null;
	}
	
	private static boolean isAlphaNumeric(String s) {
		for (int i = 0;i < s.length(); i++) {
			if (!Character.isAlphabetic(s.charAt(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean isValidAlphaInput(String username, int minLength) {
		if (username == null || username.length() < minLength) {
			return false;
		}
		
		return isAlphaNumeric(username);
	}
}