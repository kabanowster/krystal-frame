package krystal.framework.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import krystal.JSON;
import krystal.VirtualPromise;
import krystal.framework.database.implementation.Q;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
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
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> serveOptions;
	private BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> serveHead;
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
	
	public static KrystalServlet getPersistenceServlet(String context, String name, Set<PersistenceMappingInterface> mappings, String allowOrigin) {
		val origin = Map.of(
				"Access-Control-Allow-Origin", allowOrigin,
				"Access-Control-Allow-Credentials", "true"
		);
		val options = Map.of(
				"Access-Control-Allow-Origin", allowOrigin,
				"Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS",
				"Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization"
		);
		
		return KrystalServlet
				       .builder()
				       .servletName(name)
				       .servletContextString(context)
				       .addPersistenceMappings(mappings)
				       .serveGetPersistenceMappings(mappings, origin)
				       .servePostPersistenceMappings(mappings, origin)
				       .serveDeletePersistenceMappings(mappings)
				       .serveOptions((req, resp) -> VirtualPromise.run(() -> {
					       options.forEach(resp::setHeader);
					       resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
				       }))
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
		
		public KrystalServletBuilder serveDeletePersistenceMappings(Collection<PersistenceMappingInterface> mappings) {
			return this.serveDelete(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .monitor(promise -> RequestInfo.validate(promise, resp))
					                             .accept(info -> {
						                             // TODO check for auth flag
						                             
						                             if (info.patternIsMapping) {
							                             // pattern is singular with no matching-value
							                             log.debug("Invalid DELETE request - singular pattern without value: %s".formatted(req.getHttpServletMapping().getPattern()));
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             return;
						                             }
						                             try {
							                             if (info.patternIsPlural) {
								                             val statement = info.mapping.getPersistenceClass().getDeclaredConstructor().newInstance().getTable().delete().where1is1();
								                             val params = req.getParameterMap();
								                             if (!params.isEmpty()) params.forEach((k, v) -> statement.andWhere(Q.c(k).is((Object[]) v)));
								                             statement.promise().joinThrow();
							                             } else {
								                             info.mapping.getPersistenceClass().getDeclaredConstructor(String.class).newInstance(req.getHttpServletMapping().getMatchValue()).delete();
							                             }
						                             } catch (NumberFormatException e) {
							                             log.debug(e);
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						                             } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
							                             log.error(e);
							                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						                             }
					                             }));
		}
		
		public KrystalServletBuilder servePostPersistenceMappings(Collection<PersistenceMappingInterface> mappings, Map<String, String> responseHeaders) {
			return this.servePost(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .monitor(promise -> RequestInfo.validate(promise, resp))
					                             .accept(info -> {
						                             // TODO check for auth flag
						                             
						                             // prep response
						                             resp.setContentType("application/json");
						                             resp.setCharacterEncoding(StandardCharsets.UTF_8);
						                             responseHeaders.forEach(resp::setHeader);
						                             
						                             try {
							                             val str = req.getReader().lines().collect(Collectors.joining());
							                             val clazz = info.mapping.getPersistenceClass();
							                             val error = new ClassCastException("Can not perform saving execution - %s is not a PersistenceInterface.".formatted(clazz));
							                             
							                             if (info.patternIsPlural) {
								                             val body = new JSONArray(str);
								                             val results = new ArrayList<PersistenceInterface>(body.length());
								                             body.forEach(o -> {
									                             try {
										                             if (JSON.into(o, clazz) instanceof PersistenceInterface obj) {
											                             obj.save();
											                             results.add(obj);
										                             } else throw error;
									                             } catch (JSONException | ClassCastException e) {
										                             log.debug(e);
									                             }
								                             });
								                             resp.getWriter().write(JSON.fromObjects(results).toString());
							                             } else {
								                             val body = new JSONObject(str);
								                             if (JSON.into(body, clazz) instanceof PersistenceInterface obj) {
									                             obj.save();
									                             resp.getWriter().write(obj.toJSON().toString());
								                             } else throw error;
							                             }
						                             } catch (JSONException | ClassCastException e) {
							                             log.debug(e);
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						                             } catch (IOException e) {
							                             log.error(e);
							                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						                             }
					                             }));
		}
		
		public KrystalServletBuilder serveGetPersistenceMappings(Collection<PersistenceMappingInterface> mappings, Map<String, String> responseHeaders) {
			return this.serveGet(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .monitor(promise -> RequestInfo.validate(promise, resp))
					                             .accept(info -> {
						                             // TODO check for auth flag
						                             
						                             // prep response
						                             resp.setContentType("application/json");
						                             resp.setCharacterEncoding(StandardCharsets.UTF_8);
						                             responseHeaders.forEach(resp::setHeader);
						                             
						                             if (info.patternIsMapping) {
							                             // pattern is singular with no matching-value
							                             log.debug("Invalid GET request - singular pattern without value: %s".formatted(req.getHttpServletMapping().getPattern()));
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             return;
						                             }
						                             if (info.patternIsPlural) {
							                             val params = req.getParameterMap();
							                             PersistenceInterface.promiseAll(info.mapping.getPersistenceClass(), params.isEmpty() ? null : w -> {
								                                                 params.forEach((k, v) -> w.andWhere(Q.c(k).is((Object[]) v)));
								                                                 return w;
							                                                 })
							                                                 .map(stream -> {
								                                                 val invoke = info.mapping.getInvokedOnLoadFunction();
								                                                 return invoke == null ? stream : stream.map(invoke);
							                                                 })
							                                                 .map(Stream::toList)
							                                                 .map(JSON::fromObjects)
							                                                 .map(JSONArray::toString)
							                                                 .accept(result -> {
								                                                 try {
									                                                 resp.getWriter().write(result);
								                                                 } catch (IOException e) {
									                                                 log.error(e);
									                                                 resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								                                                 }
							                                                 }).joinThrow();
						                             } else {
							                             try {
								                             val id = req.getHttpServletMapping().getMatchValue();
								                             var result = info.mapping.getPersistenceClass().getDeclaredConstructor(String.class).newInstance(id);
								                             val invoke = info.mapping.getInvokedOnLoadFunction();
								                             if (invoke != null) result = invoke.apply(result);
								                             if (result.noneIsNull()) resp.getWriter().write(result.toJSON().toString());
							                             } catch (NumberFormatException e) {
								                             log.debug(e);
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
			
			// TODO authorisation flags r-w-d
			
			public RequestInfo(HttpServletRequest request, Collection<PersistenceMappingInterface> mappings) {
				String pattern = request.getHttpServletMapping().getPattern();
				patternIsPlural = pattern.matches("^/\\w+s$");
				patternIsMapping = !pattern.matches("^/\\w+(s|\\W+)$");
				val patternName = pattern.splitWithDelimiters("(?<=/)\\w+[^s\\W]", 0)[1];
				mapping = mappings.stream().filter(m -> m.name().equalsIgnoreCase(patternName)).findAny().orElseThrow(NoSuchElementException::new);
			}
			
			public static void validate(VirtualPromise<RequestInfo> info, HttpServletResponse response) {
				if (!info.hasException()) return;
				val ex = info.getException();
				if (ex instanceof NoSuchElementException) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				else {
					log.error(ex);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
				info.cancelAndDrop();
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
		if (!serveAsync(req, resp, serveGet)) super.doGet(req, resp);
	}
	
	@Override
	protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, servePatch)) super.doPatch(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, servePost)) super.doPost(req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, servePut)) super.doPut(req, resp);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, serveDelete)) super.doDelete(req, resp);
	}
	
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, serveOptions)) super.doOptions(req, resp);
	}
	
	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!serveAsync(req, resp, serveHead)) super.doHead(req, resp);
	}
	
	private boolean serveAsync(HttpServletRequest req, HttpServletResponse resp, @Nullable BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> serveAsyncAction) {
		if (serveAsyncAction == null) return false;
		
		val asyncContext = req.startAsync();
		serveAsyncAction.apply(req, resp)
		                .catchRun(ex -> {
			                log().fatal(ex.getMessage(), ex);
			                try {
				                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			                } catch (IOException ignored) {
			                }
		                })
		                .thenRun(asyncContext::complete);
		return true;
	}
	
}