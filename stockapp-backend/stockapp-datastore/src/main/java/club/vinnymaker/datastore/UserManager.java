package club.vinnymaker.datastore;

import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import club.vinnymaker.data.User;

public class UserManager {
	
	public static final int USERNAME_LENGTH_MIN = 5;
	public static final int PASSWORD_LENGTH_MIN = 8;
	
	public static final String USERNAME_INVALID_ERROR = "Username must be alphanumeric and at least 5 chars long";
	public static final String PASSWORD_INVALID_ERROR = "Password must be alphanumeric and at least 8 chars long";
	public static final String USERNAME_ALREADY_EXISTS = "Username already taken. Use another one";

	
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
		return BCrypt.gensalt();
	}
	
	private String getPasswordHash(String password, String salt) {
		return BCrypt.hashpw(password, salt);
	}
	
	private boolean verifyPassword(String candidate, String storedHash) {
		return BCrypt.checkpw(candidate, storedHash);
	}
	
	/**
	 * Load a user from database by user id. If such a user is already persistent, it's returned
	 * automatically. If no user exists with the id, null is returned.
	 *  
	 * @param userId Id of the user to load.
	 * 
	 * @return User object if loaded successfully, null otherwise.
	 */
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
	 * Load a user from db by username.
	 * 
	 * @param username Username of the user to load.
	 * 
	 * @return User object if successful, null otherwise.
	 */
	public User loadUser(String username) {
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<User> query = builder.createQuery(User.class);
			Root<User> root = query.from(User.class);
			query.select(root).where(builder.equal(root.<String>get("username"), username));
			List<User> users = session.createQuery(query).getResultList();
			if (users.size() == 0) {
				// no user exists with the given username.
				return null;
			}
			tx.commit();
			return users.get(0);
		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
			return null;
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}
	
	/**
	 * Verifies that the user details are valid and persists an updated user in the database.
	 *   
	 * @param user A detached User object whose value has just changed.
	 * @param newPassword New password for this user.
	 * 
	 * @return True if successfully updated, false otherwise.
	 * @throws InvalidUserException when the user details are not valid.
	 */
	public boolean updateUser(User user, String newPassword) throws InvalidUserException {
		// Update password hash + salt in the user object.
		String newSalt = createRandomSalt();
		String newHash = getPasswordHash(newPassword, newSalt);
		user.setPasswordHash(newHash);
		user.setPasswordSalt(newSalt);
		
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
		// Password is validated at this point. So just validate the username.
		if (!isValidAlphaInput(username, USERNAME_LENGTH_MIN)) {
			throw new InvalidUserException(USERNAME_INVALID_ERROR);
		}
		
		// Check if the username is taken already.
		if (loadUser(username) != null)  {
			throw new InvalidUserException(USERNAME_ALREADY_EXISTS);
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
			// TODO(vinay) -> Error occurred during the transaction, it must be logged.
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
	
	private static boolean isAlphaNumeric(String s) {
		for (int i = 0;i < s.length(); i++) {
			if (!Character.isAlphabetic(s.charAt(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean isValidAlphaInput(String username, int minLength) {
		if (username == null || username.length() < minLength) {
			return false;
		}
		
		return isAlphaNumeric(username);
	}
	
	/**
	 * Validate that the password.(Currently, it must be alphanumeric and 8 chars long min).
	 * 
	 * @param password Password to be validated.
	 * @return True if the input is a valid password, false otherwise.
	 */
	public static boolean isValidPassword(String password) {
		return isValidAlphaInput(password, PASSWORD_LENGTH_MIN);
	}
}
