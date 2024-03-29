package krystal.framework.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Builder around {@link HttpServlet}. Remember to define {@link #mappings}.
 */
@Builder
public class KrystalHttpServlet extends HttpServlet {
	
	/** Servlet context mappings in pattern of {@code /foo/bar} */
	@Singular private @Getter List<String> mappings;
	private @Getter String servletName;
	
	private BiConsumer<HttpServletRequest, HttpServletResponse> serveGet;
	private BiConsumer<HttpServletRequest, HttpServletResponse> servePatch;
	private BiConsumer<HttpServletRequest, HttpServletResponse> servePost;
	private BiConsumer<HttpServletRequest, HttpServletResponse> servePut;
	private BiConsumer<HttpServletRequest, HttpServletResponse> serveDelete;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (serveGet != null) {
			serveGet.accept(req, resp);
		} else {
			super.doGet(req, resp);
		}
	}
	
	@Override
	protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (servePatch != null) {
			servePatch.accept(req, resp);
		} else {
			super.doPatch(req, resp);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (servePost != null) {
			servePost.accept(req, resp);
		} else {
			super.doPost(req, resp);
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (servePut != null) {
			servePut.accept(req, resp);
		} else {
			super.doPut(req, resp);
		}
		
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (serveDelete != null) {
			serveDelete.accept(req, resp);
		} else {
			super.doDelete(req, resp);
		}
		
	}
	
}