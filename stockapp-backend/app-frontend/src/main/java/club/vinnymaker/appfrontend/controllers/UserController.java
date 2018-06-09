package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

// Controller methods must be thread safe. 
public class UserController {
	public static void getUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("key", "value");
		resp.setStatus(200);
		
		PrintWriter pw = resp.getWriter();
		pw.print(obj.toString());
		pw.close();
	}
	
	public static void createUser(HttpServletRequest req, HttpServletResponse resp) {
	}
	
	public static void getUserFavorites(HttpServletRequest req, HttpServletResponse resp) {
	}
}
