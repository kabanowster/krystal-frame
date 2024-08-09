package krystal.framework.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import krystal.JSON;
import krystal.VirtualPromise;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@Log4j2
@Builder
@WebServlet(asyncSupported = true)
public class KrystalServlet extends HttpServlet implements LoggingInterface {
	
	/**
	 * @see KrystalServlet
	 */
	private @Singular @Getter Set<String> mappings;
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
	
	
	/*
	 * Builder expansion for persistence methods
	 */
	
	public static KrystalServlet getPersistenceServlet(String context, String name, Set<PersistenceMappingInterface> mappings) {
		return KrystalServlet.builder()
		                     .servletName(name)
		                     .servletContextString(context)
		                     .addPersistenceMappings(mappings)
		                     .serveGetPersistenceMappings(mappings)
		                     .servePutPersistenceMappings(mappings)
		                     .build();
	}
	
	public static class KrystalServletBuilder {
		
		public KrystalServletBuilder addPersistenceMappings(Collection<PersistenceMappingInterface> mappings) {
			val builderMappings = Optional.ofNullable(this.mappings).orElseGet(() -> {
				this.mappings = new ArrayList<>();
				return this.mappings;
			});
			mappings.forEach(mapping -> {
				builderMappings.add(mapping.mapping());
				builderMappings.add(mapping.single());
				builderMappings.add(mapping.plural());
			});
			return this;
		}
		
		public KrystalServletBuilder servePutPersistenceMappings(Collection<PersistenceMappingInterface> mappings) {
			return this.servePut(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .accept(info -> {
						                             if (!info.patternIsMapping) {
							                             // pattern is plural or with matching-value ("/*")
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             return;
						                             }
						                             try {
							                             val body = req.getReader().lines().collect(Collectors.joining());
							                             if (JSON.into(body, info.mapping.getPersistenceClass()) instanceof PersistenceInterface obj) obj.save();
						                             } catch (IOException e) {
							                             log.error(e);
							                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						                             }
					                             }));
		}
		
		public KrystalServletBuilder serveGetPersistenceMappings(Collection<PersistenceMappingInterface> mappings) {
			return this.serveGet(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .accept(info -> {
						                             resp.setContentType("application/json");
						                             if (info.patternIsMapping) {
							                             // pattern is singular with no matching-value
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             return;
						                             }
						                             if (info.patternIsPlural) {
							                             // TODO parameters as filters
							                             PersistenceInterface.promiseAll(info.mapping.getPersistenceClass())
							                                                 .map(Stream::toList)
							                                                 .map(JSON::from)
							                                                 .map(JSONObject::toString)
							                                                 .accept(result -> {
								                                                 try {
									                                                 resp.getWriter().write(result);
								                                                 } catch (IOException e) {
									                                                 log.error(e);
									                                                 resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								                                                 }
							                                                 })
							                                                 .joinThrow();
						                             } else {
							                             try {
								                             val id = req.getHttpServletMapping().getMatchValue();
								                             val result = info.mapping.getPersistenceClass().getDeclaredConstructor(String.class).newInstance(id);
								                             if (result.noneIsNull())
									                             resp.getWriter().write(result.toJSON().toString());
								                             else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
								                             
							                             } catch (NumberFormatException e) {
								                             log.error(e);
								                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException e) {
								                             log.error(e);
								                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							                             }
						                             }
					                             }));
		}
		
		private static class RequestInfo {
			
			private final PersistenceMappingInterface mapping;
			private final boolean patternIsPlural;
			private final boolean patternIsMapping;
			
			public RequestInfo(HttpServletRequest request, Collection<PersistenceMappingInterface> mappings) {
				String pattern = request.getHttpServletMapping().getPattern();
				patternIsPlural = pattern.matches("^/\\w+s$");
				patternIsMapping = !pattern.matches("^/\\w+(s|\\W+)$");
				val patternName = pattern.splitWithDelimiters("(?<=/)\\w+[^s\\W]", 0)[1].toLowerCase();
				mapping = mappings.stream()
				                  .filter(m -> m.name().equals(patternName))
				                  .findAny()
				                  .orElseThrow(NoSuchElementException::new);
			}
			
		}
		
	}
	
	
	/*
	 * HttpServlet-specific overrides
	 */
	
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