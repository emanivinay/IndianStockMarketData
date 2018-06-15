package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import club.vinnymaker.data.User;
import club.vinnymaker.datastore.InvalidUserException;
import club.vinnymaker.datastore.UserManager;

import static club.vinnymaker.datastore.UserManager.PASSWORD_INVALID_ERROR;

// Controller methods must be thread safe. 
public class UserController extends BaseController {
	
	private static final String PASSWORD_KEY = "password";
	private static final String PASSWORD_NOT_PRESENT_ERROR = "'password' key is required";
	private static final String USERNAME_KEY = "username";
	private static final String USER_NOT_FOUND = "No such user found";
	
	private static final DateFormat USER_DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy KK:mm:ss a Z");
	private static final String GMT_TIMEZONE_ID = "GMT+00:00";
	
	static {
		// Set user date format to GMT timezone.
		USER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(GMT_TIMEZONE_ID));
	}
	
	/**
	 * Fetches the user details with the given id and returns a JSON response.
	 * 
	 * @param req HTTP GET request for the user.
	 * @param resp JSON response with user details will be returned.
	 * @param namedParams Route parameters (e.g., id) in this request.
	 * @throws IOException
	 */
	public static void getUser(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
		String username = namedParams.get(USERNAME_KEY);
		if (!authenticate(req, username)) {
			// send 401.
			authFailure(resp);
			return;
		}
		
		User user = UserManager.getInstance().loadUser(username);
		if (user == null) {
			// no such user found, return 404.
			error(resp, HttpServletResponse.SC_NOT_FOUND, USER_NOT_FOUND);
			return;
		}
		
		JSONObject obj = new JSONObject(user);
		// remove sensitive items from this object.
		obj.remove("passwordHash");
		obj.remove("passwordSalt");
		obj.put("dateCreated", getDateInUTC(user.getDateCreated()));
		success(resp, obj);
	}
	
	private static String getDateInUTC(Date date) {
		return USER_DATE_FORMAT.format(date);
	}
	
	/**
	 * Creates or updates or deletes a user from the database, depending upon the request. A new user will be created
	 * if no id is present in request body and username, password are present and valid. if 'delete' key is present, user 
	 * with the given id will be deleted. Otherwise, an existing user account is updated.
	 * 
	 * @param req HTTP POST requests are required for creating/updating/deleting users. 
	 * @param resp HTTP response is sent as json.
	 * @param namedParams Any named parameters in the route.
	 * @throws IOException
	 */
	public static void createOrUpdateUser(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
		boolean isDeletion = req.getParameter(DELETE_KEY) != null;
		boolean isUserNamePresent = req.getParameter(USERNAME_KEY) != null;
		boolean isCreation = req.getParameter(CREATE_KEY) != null;
		
		if (isDeletion) {
			if (!isUserNamePresent) {
				// delete requests should have id present. Return an invalid request error.
				error(resp, HttpServletResponse.SC_BAD_REQUEST, ID_NOT_PRESENT_ERROR);
			} else {
				// Username present.
				String username = req.getParameter(USERNAME_KEY);
				
				// authenticate first.
				if (!authenticate(req, username)) {
					authFailure(resp);
					return;
				}
				
				if (UserManager.getInstance().deleteUser(username)) {
					// successfully deleted the user.
					JSONObject body = new JSONObject();
					body.put(SUCCESS_KEY, "true");
					success(resp, body);
				} else {
					// Failed deleting the user, most likely user doesn't exist.
					error(resp, HttpServletResponse.SC_NOT_FOUND, RESOURCE_DOESNT_EXIST_ERROR);
				}
			}
			
			return;
		} 
		
		// Both creation and updating require valid username.
		String username = req.getParameter(USERNAME_KEY);
		
		
		// Get the password and check if it's valid.
		if (req.getParameter(PASSWORD_KEY) == null) {
			// Password is mandatory either for updating existing or creating new users.
			error(resp, HttpServletResponse.SC_BAD_REQUEST, PASSWORD_NOT_PRESENT_ERROR);
			return;
		}
		
		// Validate the new password first.
		String password = req.getParameter(PASSWORD_KEY);
		if (!UserManager.isValidPassword(password)) {
			error(resp, HttpServletResponse.SC_BAD_REQUEST, PASSWORD_INVALID_ERROR);
			return;
		}
		
		if (isCreation) {
			// New user creation.
			// TODO(vinay) -> User creation must validate a secret key first. 
			Long userId = null;
			try {
				userId = UserManager.getInstance().createUser(username, password);
			} catch (InvalidUserException e) {
				error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				return;
			}
			
			if (userId == null) {
				// failed creating a new user. Return an internal error.
				internal_error(resp);
			} else {
				// successfully created a new user. Return success status.
				JSONObject body = new JSONObject();
				body.put(SUCCESS_KEY, "true");
				success(resp, body);
			}
		} else {
			// Try updating an existing user, after authenticating first.
			if (!authenticate(req, username)) {
				authFailure(resp);
				return;
			}
			
			User user = UserManager.getInstance().loadUser(username);
			if (user == null) {
				// No such existing user, return 404.
				error(resp, HttpServletResponse.SC_NOT_FOUND, USER_NOT_FOUND);
				return;
			}
			
			if (UserManager.getInstance().updateUser(user, password)) {
				JSONObject body = new JSONObject();
				body.put(SUCCESS_KEY, "true");
				success(resp, body);
			} else {
				internal_error(resp);
			}
		}
	}
	
	public static void getUserFavorites(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
	}
}
