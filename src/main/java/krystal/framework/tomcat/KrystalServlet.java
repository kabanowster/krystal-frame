package krystal.framework.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import krystal.VirtualPromise;
import krystal.framework.logging.LoggingInterface;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.val;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builder of asynchronous (non-blocking) {@link HttpServlet}, utilising Virtual Threads. Remember to define {@link #mappings}.
 * API context pattern: {@code server:port/contextPath/servletMapping}.
 * <dl>
 *     <dt><b><i>mappings</i></b></dt><dd>Url patterns of {@code /foo/bar}, the servlet will respond to, after {@link #servletContextString}. If any not provided - <code>default: <i>"/"</i></code></dd>
 *     <dt><b><i>initParameters</i></b></dt><dd>Equivalents of {@link HttpServlet#getInitParameter(String)}.</dd>
 *     <dt><b><i>servletName</i></b></dt><dd><code>Default: <i>unnamedServlet</i></code></dd>
 *     <dt><b><i>servletContextString</i></b></dt><dd>Root mapping for Tomcat context. <br /><code>Default: {@link TomcatProperties#getDefaultServletsContext()} = <i>"/api"</i></code></dd>
 *     <dt><b><i>init</i></b></dt><dd>To run as {@link HttpServlet#init()}.</dd>
 *     <dt><b><i>destroy</i></b></dt><dd>To run as {@link HttpServlet#destroy()}.</dd>
 * </dl>
 *
 * @see VirtualPromise
 */
@Builder
@WebServlet(asyncSupported = true)
public class KrystalServlet extends HttpServlet implements LoggingInterface {
	
	/**
	 * @see KrystalServlet
	 */
	private @Singular @Getter List<String> mappings;
	/**
	 * @see KrystalServlet
	 */
	private @Singular Map<String, String> initParameters;
	/**
	 * @see KrystalServlet
	 */
	private @Getter String servletName;
	/**
	 * @see KrystalServlet
	 */
	private @Getter String servletContextString;
	
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> serveGet;
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> servePatch;
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> servePost;
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> servePut;
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> serveDelete;
	/**
	 * @see KrystalServlet
	 */
	private Consumer<KrystalServlet> init;
	/**
	 * @see KrystalServlet
	 */
	private Consumer<KrystalServlet> destroy;
	
	@Override
	public void init() throws ServletException {
		if (init != null) {
			init.accept(this);
		} else {
			super.init();
		}
	}
	
	@Override
	public void destroy() {
		if (destroy != null) {
			destroy.accept(this);
		} else {
			super.destroy();
		}
	}
	
	@Override
	public String getInitParameter(String name) {
		return initParameters.get(name);
	}
	
	@Override
	public Enumeration<String> getInitParameterNames() {
		return new Vector<>(initParameters.keySet()).elements();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, serveGet))
			super.doGet(req, resp);
	}
	
	@Override
	protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, servePatch))
			super.doPatch(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, servePost))
			super.doPost(req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, servePut))
			super.doPut(req, resp);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, serveDelete))
			super.doDelete(req, resp);
	}
	
	private boolean serveAsync(HttpServletRequest req, HttpServletResponse resp, @Nullable BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> serveAsyncAction) {
		if (serveAsyncAction == null)
			return false;
		
		val asyncContext = req.startAsync();
		serveAsyncAction.apply(req, resp)
		                .catchRun(ex -> {
			                log().fatal(ex.getMessage());
			                try {
				                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			                } catch (IOException ignored) {
			                }
		                })
		                .thenRun(asyncContext::complete);
		
		return true;
	}
	
}