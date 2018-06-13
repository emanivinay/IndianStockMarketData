package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import club.vinnymaker.appfrontend.RoutingServlet;
import club.vinnymaker.data.User;
import club.vinnymaker.datastore.InvalidUserException;
import club.vinnymaker.datastore.UserManager;

import static club.vinnymaker.datastore.UserManager.PASSWORD_INVALID_ERROR;

// Controller methods must be thread safe. 
public class UserController extends BaseController {
	
	private static final String PASSWORD_KEY = "password";
	private static final String PASSWORD_NOT_PRESENT_ERROR = "'password' key is required";
	private static final String USERNAME_KEY = "username";
	private static final String USERNAME_NOT_PRESENT_ERROR = "'username' key is required";
	
	/**
	 * Fetches the user details with the given id and returns a JSON response.
	 * 
	 * @param req HTTP GET request for the user.
	 * @param resp JSON response with user details will be returned.
	 * @param namedParams Route parameters (e.g., id) in this request.
	 * @throws IOException
	 */
	public static void getUser(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
		long userId = Long.parseLong(namedParams.get(ID_KEY));
		User user = UserManager.getInstance().loadUser(userId);
		if (user == null) {
			// no such user found, return 404.
			RoutingServlet.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "No such user found");
			return;
		}
		
		JSONObject obj = new JSONObject(user);
		// remove sensitive items from this object.
		obj.remove("passwordHash");
		obj.remove("passwordSalt");
		success(resp, obj);
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
		boolean isUserIdPresent = req.getParameter(ID_KEY) != null;
		
		if (isDeletion) {
			if (!isUserIdPresent) {
				// delete requests should have id present. Return an invalid request error.
				error(resp, HttpServletResponse.SC_BAD_REQUEST, ID_NOT_PRESENT_ERROR);
			} else {
				// User id is present.
				Long userId = Long.parseLong(req.getParameter(ID_KEY));
				if (UserManager.getInstance().deleteUser(userId)) {
					// successfully deleted the user.
					success(resp, EMPTY_JSON_OBJ);
				} else {
					// Failed deleting the user, most likely user doesn't exist.
					error(resp, HttpServletResponse.SC_NOT_FOUND, RESOURCE_DOESNT_EXIST_ERROR);
				}
			}
		} else {
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
			
			if (!isUserIdPresent) {
				// New user creation. Must have a valid username.
				boolean isUsernamePresent = req.getParameter(USERNAME_KEY) != null;
				if (!isUsernamePresent) {
					error(resp, HttpServletResponse.SC_BAD_REQUEST, USERNAME_NOT_PRESENT_ERROR);
					return;
				}
				
				String username = req.getParameter(USERNAME_KEY);
				Long newUserId = null;
				try {
					newUserId = UserManager.getInstance().createUser(username, password);
					if (newUserId == null) {
						// error creating user. return an internal server error.
						internal_error(resp);
					} else {
						// successfully created the user, return 200 with empty json.
						success(resp, EMPTY_JSON_OBJ);
					}
				} catch (InvalidUserException e) {
					// username/password are wrong, return a 400 response.
					error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				}
				
			} else {
				Long userId = Long.parseLong(req.getParameter(ID_KEY));
				// Update existing user. Currently, only passwords can be updated.
				User user = UserManager.getInstance().loadUser(userId);
				try {
					if (UserManager.getInstance().updateUser(user)) {
						// successfully updated the user.
						success(resp, EMPTY_JSON_OBJ);
					} else {
						// failed updating the user.
						internal_error(resp);
					}
				} catch (InvalidUserException e) {
					error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
				}
			}
		}
	}
	
	public static void getUserFavorites(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
	}
}
