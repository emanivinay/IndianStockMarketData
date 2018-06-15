package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import club.vinnymaker.appfrontend.RoutingServlet;
import club.vinnymaker.datastore.UserManager;

/**
 * A Controller is a piece of code that services requests for a specific type of a resource. 
 * BaseController provides functionality common to all controllers.
 * 
 * @author evinay
 *
 */
public abstract class BaseController {
	
	public static final String INTERNAL_SERVER_ERROR = "Internal server error";
	public static final String ID_KEY = "id";
	public static final String DELETE_KEY = "delete";
	public static final String CREATE_KEY = "create";
	public static final String ID_NOT_PRESENT_ERROR = "This request must have a valid id key";
	public static final String RESOURCE_DOESNT_EXIST_ERROR = "The specified resource doesn't exist";
	public static final String SUCCESS_KEY = "success";
	public static final String INVALID_AUTH_ERROR = "Incorrect authentication details";
	
	private static final String AUTH_HEADER_NAME = "Authorization";
	private static final String BASIC_PREFIX = "Basic ";
	private static final int BASIC_PREFIX_LEN = 6;

	
	protected static final JSONObject EMPTY_JSON_OBJ = new JSONObject();
	
	/**
	 * Constructs a successful response with a body.
	 * 
	 * @param resp
	 * @param content
	 */
	protected static void success(HttpServletResponse resp, JSONObject content) throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		PrintWriter pw = resp.getWriter();
		pw.write(content.toString());
		pw.close();
	}
	
	protected static void authFailure(HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		PrintWriter pw = resp.getWriter();
		JSONObject body = new JSONObject();
		body.put(RoutingServlet.ERROR_CODE_KEY, INVALID_AUTH_ERROR);
		pw.write(body.toString());
		pw.close();
	}
	
	/**
	 * Constructs an error JSON response with appropriate response code and status code. Delegates to 
	 * RoutingServlet.sendError
	 * 
	 * @param resp HTTP response to be returned in JSON format.
	 * @param statusCode HTTP error status - 404, 400 etc...
	 * @param reason A string describing the reason for this error.
	 */
	protected static void error(HttpServletResponse resp, int statusCode, String reason) throws IOException {
		RoutingServlet.sendError(resp, statusCode, reason);
	}
	
	/**
	 * Constructs a json response in case of a internal server error.
	 * 
	 * @param resp HTTP 500 response. 
	 * @throws IOException
	 */
	protected static void internal_error(HttpServletResponse resp) {
		resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		try {
			PrintWriter pw = resp.getWriter();
			JSONObject obj = new JSONObject();
			obj.put(RoutingServlet.ERROR_CODE_KEY, INTERNAL_SERVER_ERROR);
			pw.write(obj.toString());
			pw.close();
		} catch (IOException e) {
			// TODO(vinay) -> Most likely, connection got closed. Log this error.
		}
	}
	
	/**
	 * Authenticate the request with basic HTTP auth.
	 *  
	 * @param req The http request.
	 * @param username user whose password is to be authenticated.
	 * 
	 * @return True if successful, false otherwise.
	 */
	protected static boolean authenticate(HttpServletRequest req, String username) {
		String authHeader = req.getHeader(AUTH_HEADER_NAME);
		if (authHeader == null || !authHeader.startsWith(BASIC_PREFIX)) {
			return false;
		}
		
		String authStripped = authHeader.substring(BASIC_PREFIX_LEN);
		byte[] decodedBytes = Base64.getDecoder().decode(authStripped);
		String decodedString = new String(decodedBytes);
		String[] parts = decodedString.split(":");
		if (parts.length != 2) {
			return false;
		}
		
		String usernameInAuthHeader = parts[0], password = parts[1];
		if (!username.equals(usernameInAuthHeader)) {
			return false;
		}
		
		return UserManager.getInstance().verifyUserPassword(username, password);
	}
}
