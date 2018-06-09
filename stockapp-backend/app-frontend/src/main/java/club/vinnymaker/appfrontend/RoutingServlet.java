package club.vinnymaker.appfrontend;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bigtesting.routd.Route;
import org.bigtesting.routd.Router;
import org.bigtesting.routd.TreeRouter;

import club.vinnymaker.appfrontend.controllers.IController;
import club.vinnymaker.appfrontend.controllers.UserController;
import lombok.Getter;

public class RoutingServlet extends HttpServlet {
	private static final long serialVersionUID = 956003895642907877L;
	private static Router router;
	
	public static final String INCORRECT_REQUEST_METHOD_ERROR = "Wrong request method for uri - ";
	public static final String INVALID_ROUTE = "Invalid uri"; 
	
	@Getter
	static class APIRoute extends Route {
		private final String requestMethod;
		private final IController controller;
		
		public APIRoute(String path, String method,  IController controller) {
			super(path);
			requestMethod = method;
			this.controller = controller;
		}
	}
	
	static {
		router = new TreeRouter();
		router.add(new APIRoute("/user/:id<[0-9]+>", "GET", UserController::getUser));
		router.add(new APIRoute("/user/create", "POST", UserController::createUser));
		router.add(new APIRoute("/user/:id<[0-9]+>/favorites", "GET", UserController::getUserFavorites));
	}
	
	private static void checkRequestAndThrow(HttpServletRequest req) throws ServletException {
		Route route = router.route(req.getPathInfo());
		if (!(route instanceof APIRoute)) {
			throw new ServletException(INVALID_ROUTE + " " + req.getRequestURI());
		}
		
		APIRoute aroute = (APIRoute) route;
		if (!aroute.getRequestMethod().equals(req.getMethod())) {
			throw new ServletException(INCORRECT_REQUEST_METHOD_ERROR + req.getRequestURI());
		}
	}
	
	// Does some sanity checking, set some response headers and call the controller method.
	private static void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		checkRequestAndThrow(req);
		APIRoute route = (APIRoute) router.route(req.getPathInfo());
		resp.setHeader("Content-Type", "application/json");
		route.getController().view(req, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handle(req, resp);
	}
}
