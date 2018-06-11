package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import club.vinnymaker.appfrontend.RoutingServlet;

// Controller methods must be thread safe. 
public class UserController extends BaseController {
	public static void getUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("pathInfo", req.getPathInfo());
		resp.setStatus(200);
		
		PrintWriter pw = resp.getWriter();
		pw.print(obj.toString());
		pw.close();
	}
	
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String INCOMPLETE_PARAMETER_LIST = "Username/password parameters must be present in request";
	
	public static void createUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Required parameters are username and password.
		if (req.getParameter(USERNAME) == null || req.getParameter(PASSWORD) == null) {
			RoutingServlet.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, INCOMPLETE_PARAMETER_LIST);
		}
	}
	
	public static void getUserFavorites(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	}
}
