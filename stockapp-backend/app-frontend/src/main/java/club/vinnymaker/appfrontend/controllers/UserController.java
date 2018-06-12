package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import club.vinnymaker.appfrontend.RoutingServlet;
import club.vinnymaker.data.User;
import club.vinnymaker.datastore.UserManager;

// Controller methods must be thread safe. 
public class UserController extends BaseController {
	
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String INCOMPLETE_PARAMETER_LIST = "Username/password parameters must be present in request";
	
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
	
	public static void createUser(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
		// Required parameters are username and password.
		if (req.getParameter(USERNAME) == null || req.getParameter(PASSWORD) == null) {
			RoutingServlet.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, INCOMPLETE_PARAMETER_LIST);
			return;
		}

		String username = req.getParameter(USERNAME);
		String password = req.getParameter(PASSWORD);
		Long newUserId = UserManager.getInstance().createUser(username, password);
		System.out.println("newUserId is " + newUserId);
		JSONObject body = new JSONObject();
		body.put(ID_KEY, newUserId);
		success(resp, body);
	}
	
	public static void getUserFavorites(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException {
	}
}
