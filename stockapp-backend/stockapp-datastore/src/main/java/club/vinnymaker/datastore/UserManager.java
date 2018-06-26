package club.vinnymaker.datastore;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mindrot.jbcrypt.BCrypt;

import club.vinnymaker.data.User;

/**
 * CRUD layer for {@link User} objects in the database.   
 * 
 * @author evinay
 *
 */
public class UserManager {
	
	public static final int USERNAME_LENGTH_MIN = 5;
	public static final int PASSWORD_LENGTH_MIN = 8;
	
	public static final String USERNAME_INVALID_ERROR = "Username must be alphanumeric and at least 5 chars long";
	public static final String PASSWORD_INVALID_ERROR = "Password must be alphanumeric and at least 8 chars long";
	public static final String USERNAME_ALREADY_EXISTS = "Username already taken. Use another one";
	
	private static final Pattern USERNAME_PAT = Pattern.compile("[0-9A-Za-z_-]+");

	private static final Logger logger = LogManager.getLogger(UserManager.class);
	
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
	 * Verify a given user's password.
	 * 
	 * @param username Username whose password should be verified.
	 * @param password Candidate password.
	 * @return True if correct password, false otherwise.
	 */
	public boolean verifyUserPassword(String username, String password) {
		User user = loadUser(username);
		if (user == null) {
			return false;
		}
		
		String hash = user.getPasswordHash();
		return verifyPassword(password, hash);
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
	 * @return User object if a exists with the username or null otherwise.
	 */
	public User loadUser(String username) {
		if (!isValidTextInput(username, USERNAME_LENGTH_MIN)) {
			// Not a valid username, so user cannot exist.
			return null;
		}
		
		Session session = DataStoreManager.getInstance().getFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<User> query = builder.createQuery(User.class);
			Root<User> root = query.from(User.class);
			query.select(root).where(builder.equal(root.<String>get("username"), username));
			List<User> users = session.createQuery(query).getResultList();
			tx.commit();
			if (users.size() == 0) {
				// no user exists with the given username.
				return null;
			}
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
	public boolean updateUser(User user, String newPassword) {
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
			logger.error("Error while updating a user named " + user.getUsername());
			logger.error("Cause is " + e.getMessage());
			
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
	 * @param username Username of the user to be deleted.
	 * @return True if user successfully deleted and false otherwise(usually means no such user existed).
	 */
	public boolean deleteUser(String username) {
		User user = loadUser(username);
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
			logger.error("Error deleting the user with username " + username);
			logger.error("Cause is " + e.getMessage());
			
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
		if (!isValidTextInput(username, USERNAME_LENGTH_MIN)) {
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
			logger.error("Error creating a new user named " + username);
			logger.error("Cause is " + e.getMessage());
			
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
	 * Checks whether the given username is valid. Valid user names are composed of english
	 * letters, digits, hyphen and underscore and are of at least 5 letters long.
	 * 
	 * @param username Username to check.
	 * @param minLength Minimum length of a username.
	 * @return
	 */
	public static boolean isValidTextInput(String username, int minLength) {
		if (username == null || username.length() < minLength) {
			return false;
		}
		
		return USERNAME_PAT.matcher(username).matches();
	}
	
	/**
	 * Validate that the password.(Currently, it must be alphanumeric and 8 chars long min).
	 * 
	 * @param password Password to be validated.
	 * @return True if the input is a valid password, false otherwise.
	 */
	public static boolean isValidPassword(String password) {
		return isValidTextInput(password, PASSWORD_LENGTH_MIN);
	}
}
