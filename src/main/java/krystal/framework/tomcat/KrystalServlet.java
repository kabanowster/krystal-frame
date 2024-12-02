package krystal.framework.tomcat;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import krystal.JSON;
import krystal.VirtualPromise;
import krystal.framework.database.persistence.Persistence;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.filters.PersistenceFilters;
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
				       .serveDeletePersistenceMappings(mappings, origin)
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
		
		public KrystalServletBuilder serveGetPersistenceMappings(Collection<PersistenceMappingInterface> mappings, Map<String, String> responseHeaders) {
			return this.serveGet(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .monitor(promise -> RequestInfo.validate(promise, resp))
					                             .accept(info -> {
						                             // prep response
						                             prepStandardResponseWithHeaders(resp, responseHeaders);
						                             
						                             if (info.patternIsMapping) {
							                             // pattern is singular with no matching-value present
							                             log.debug("Invalid GET request - singular pattern without value: %s".formatted(req.getHttpServletMapping().getPattern()));
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             return;
						                             }
						                             if (info.patternIsPlural) {
							                             val params = req.getParameterMap();
							                             val clazz = info.mapping.getPersistenceClass();
							                             
							                             val loader = Persistence.promiseAll(clazz, params.isEmpty() ? null : PersistenceFilters.fromParams(params))
							                                                     .map(Stream::toList)
							                                                     .map(JSON::fromObjects)
							                                                     .map(JSONArray::toString)
							                                                     .accept(result -> {
								                                                     if (result.length() > 2) {
									                                                     try {
										                                                     resp.getWriter().write(result);
									                                                     } catch (IOException e) {
										                                                     log.error(e);
										                                                     resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                                                     }
								                                                     } else {
									                                                     resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
								                                                     }
							                                                     }).catchRun(e -> {
										                             log.error(e);
										                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                             });
							                             watchResponse(loader, resp);
							                             loader.join();
						                             } else {
							                             try {
								                             val id = req.getHttpServletMapping().getMatchValue();
								                             var result = info.mapping.getPersistenceClass().getDeclaredConstructor(String.class).newInstance(id); // TODO include doc explanation for String argument constructor required for persistence
								                             if (result.noneIsNull()) resp.getWriter().write(result.toJSON().toString());
								                             else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
		
		public KrystalServletBuilder serveDeletePersistenceMappings(Collection<PersistenceMappingInterface> mappings, Map<String, String> responseHeaders) {
			return this.serveDelete(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .monitor(promise -> RequestInfo.validate(promise, resp))
					                             .accept(info -> {
						                             if (info.patternIsMapping) {
							                             // pattern is singular with no matching-value
							                             log.debug("Invalid DELETE request - singular pattern without value: %s".formatted(req.getHttpServletMapping().getPattern()));
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             return;
						                             }
						                             // prep response
						                             prepStandardResponseWithHeaders(resp, responseHeaders);
						                             
						                             try {
							                             val clazz = info.mapping.getPersistenceClass();
							                             
							                             if (info.patternIsPlural) {
								                             val params = req.getParameterMap();
								                             if (!params.isEmpty()) {
									                             // parameterized call
									                             clazz.getDeclaredConstructor().newInstance()
									                                  .getTable()
									                                  .delete()
									                                  .where(params)
									                                  .promise()
									                                  .catchRun(e -> {
										                                  log.error(e);
										                                  resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                                  })
									                                  .join();
								                             } else {
									                             // objects posted
									                             val str = req.getReader().lines().collect(Collectors.joining());
									                             val results = processBodyOfArray(new JSONArray(str), clazz, PersistenceInterface::delete);
									                             resp.getWriter().write(JSON.fromObjects(results).toString());
								                             }
							                             } else {
								                             // single object by /id
								                             val obj = clazz.getDeclaredConstructor(String.class).newInstance(req.getHttpServletMapping().getMatchValue());
								                             obj.delete();
								                             resp.getWriter().write(obj.toJSON().toString());
							                             }
						                             } catch (NumberFormatException | JSONException | ClassCastException e) {
							                             log.debug(e);
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						                             } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException e) {
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
						                             // prep response
						                             prepStandardResponseWithHeaders(resp, responseHeaders);
						                             
						                             try {
							                             val str = req.getReader().lines().collect(Collectors.joining());
							                             val clazz = info.mapping.getPersistenceClass();
							                             
							                             if (info.patternIsPlural) {
								                             val results = processBodyOfArray(new JSONArray(str), clazz, PersistenceInterface::save);
								                             resp.getWriter().write(JSON.fromObjects(results).toString());
							                             } else {
								                             processSingleJsonElement(new JSONObject(str), clazz, element -> {
									                             try {
										                             element.save();
										                             resp.getWriter().write(element.toJSON().toString());
									                             } catch (Exception e) {
										                             log.error(e);
										                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                             }
								                             });
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
		
		private List<PersistenceInterface> processBodyOfArray(JSONArray jsonArray, Class<? extends PersistenceInterface> clazz, Consumer<PersistenceInterface> process) throws JSONException, ClassCastException {
			val results = new ArrayList<PersistenceInterface>(jsonArray.length());
			for (var json : jsonArray) processSingleJsonElement(json, clazz, process.andThen(results::add));
			return results;
		}
		
		private void processSingleJsonElement(Object json, Class<? extends PersistenceInterface> clazz, Consumer<PersistenceInterface> process) throws JSONException, ClassCastException {
			if (JSON.into(json, clazz) instanceof PersistenceInterface obj) {
				process.accept(obj);
			} else throw new ClassCastException("Can not perform persistence execution on provided request's body element - %s is not a PersistenceInterface.".formatted(clazz));
		}
		
		private void prepStandardResponseWithHeaders(HttpServletResponse response, Map<String, String> headers) {
			response.setContentType("application/json");
			response.setCharacterEncoding(StandardCharsets.UTF_8);
			headers.forEach(response::setHeader);
		}
		
		/**
		 * Stops loading if response times-out or is cancelled.
		 */
		private static void watchResponse(VirtualPromise<Void> loader, HttpServletResponse response) {
			VirtualPromise.run(() -> {
				while (loader.isAlive()) {
					try {
						// invoking this on timed-out response throws an IllegalStateException
						response.getWriter();
					} catch (Exception e) {
						log.error("Response cancelled due to timeout.", e);
						loader.cancelAndDrop();
						response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException _) {
					}
				}
			});
		}
		
		private static class RequestInfo {
			
			private final PersistenceMappingInterface mapping;
			private final boolean patternIsPlural;
			private final boolean patternIsMapping;
			
			public RequestInfo(HttpServletRequest request, Collection<PersistenceMappingInterface> mappings) {
				String pattern = request.getHttpServletMapping().getPattern();
				mapping = mappings.stream().filter(m -> m.matches(pattern)).findAny().orElseThrow(NoSuchElementException::new);
				
				patternIsPlural = pattern.equals(mapping.plural());
				patternIsMapping = pattern.equals(mapping.mapping());
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
		                .thenRun(asyncContext::complete)
		                .thenRun(System::gc);
		return true;
	}
	
}