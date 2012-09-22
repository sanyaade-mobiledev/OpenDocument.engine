package at.tomtasche.reader.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import at.tomtasche.reader.engine.model.State;

import com.google.gson.Gson;

@SuppressWarnings("serial")
public class StartPageServlet extends DriveServlet {

	/**
	 * Ensure that the user is authorized, and setup the required values for
	 * index.jsp.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		// Deserialize the state in order to specify some values to the DrEdit
		// JavaScript client below.
		Collection<String> ids = new ArrayList<String>();
		// Assume an empty ID in the list if no IDs were set.
		if (req.getParameter("state") != null) {
			State state = new State(req.getParameter("state"));
			if (state.ids != null && state.ids.size() > 0) {
				ids = state.ids;
			}
		}

		if (ids.isEmpty()) {
			resp.sendRedirect(resp
					.encodeRedirectURL("https://chrome.google.com/webstore/detail/jpcfmmdlhndnfpagbmhbbfehenapoich"));
		} else {
			getClientId(req, resp);

			req.setAttribute("ids", new Gson().toJson(ids).toString());
			req.getRequestDispatcher("/index.jsp").forward(req, resp);
		}
	}
}