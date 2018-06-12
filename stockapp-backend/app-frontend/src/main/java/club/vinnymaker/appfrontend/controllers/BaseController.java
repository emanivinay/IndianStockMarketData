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
	
	/**
	 * Constructs a successful response with a body.
	 * @param resp
	 * @param content
	 */
	public static void success(HttpServletResponse resp, JSONObject content) throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		PrintWriter pw = resp.getWriter();
		pw.write(content.toString());
		pw.close();
	}
	
	/**
	 * Constructs a json response in case of a internal server error.
	 * @param resp
	 * @throws IOException
	 */
	public static void internal_error(HttpServletResponse resp) {
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
