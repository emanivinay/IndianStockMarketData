package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import club.vinnymaker.appfrontend.RoutingServlet;

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
	public static final String ID_NOT_PRESENT_ERROR = "This request must have a valid id key";
	public static final String RESOURCE_DOESNT_EXIST_ERROR = "The specified resource doesn't exist";

	
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
	 * @param resp
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
}
