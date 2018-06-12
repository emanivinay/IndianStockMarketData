package club.vinnymaker.appfrontend.controllers;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IController {
	public void view(HttpServletRequest req, HttpServletResponse resp, Map<String, String> namedParams) throws IOException;
}
