package krystal.framework.tomcat;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import krystal.JSON;
import krystal.VirtualPromise;
import krystal.VirtualPromise.ExceptionsHandler;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.database.persistence.Persistence;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.filters.PersistenceFilters;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.logging.log4j.util.TriConsumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
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
public class KrystalServlet extends HttpServlet {
	
	private static final Responses RESPONSES = new Responses(new ConcurrentHashMap<>(), new ArrayList<>(), new AtomicReference<>(), new ReentrantLock());
	private static final Contexts CONTEXTS = new Contexts(new ConcurrentHashMap<>(), new ArrayList<>(), new AtomicReference<>(), new ReentrantLock());
	
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
		return KrystalServlet.getPersistenceServletBuilder(context, name, mappings, allowOrigin, null).build();
	}
	
	public static KrystalServletBuilder getPersistenceServletBuilder(String context,
	                                                                 String name,
	                                                                 Set<PersistenceMappingInterface> mappings,
	                                                                 String allowOrigin,
	                                                                 @Nullable Map<String, BeforeAndAfter> beforeAndAfterMethods) {
		
		val origin = Map.of(
				"Access-Control-Allow-Origin", allowOrigin,
				"Access-Control-Allow-Credentials", "true"
		);
		val options = Map.of(
				"Access-Control-Allow-Origin", allowOrigin,
				"Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS",
				"Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization"
		);
		
		return KrystalServlet.builder()
		                     .servletName(name)
		                     .servletContextString(context)
		                     .addCountableMappings(mappings)
		                     .serveGetPersistenceMappings(mappings, origin, beforeAndAfterMethods != null ? beforeAndAfterMethods.get("GET") : null)
		                     .servePostPersistenceMappings(mappings, origin, beforeAndAfterMethods != null ? beforeAndAfterMethods.get("POST") : null)
		                     .serveDeletePersistenceMappings(mappings, origin, beforeAndAfterMethods != null ? beforeAndAfterMethods.get("DELETE") : null)
		                     .serveOptions((req, resp) -> VirtualPromise.run(() -> {
			                     options.forEach(resp::setHeader);
			                     resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		                     }).thenClose());
	}
	
	/*
	 * Servlet actions
	 */
	
	/**
	 * @param after
	 * 		Object is either {@link PersistenceInterface}, {@link List} of {@link PersistenceInterface}, or {@link Optional} of {@link QueryResultInterface}.
	 */
	public record BeforeAndAfter(@Nullable BiConsumer<HttpServletRequest, HttpServletResponse> before, @Nullable TriConsumer<HttpServletRequest, HttpServletResponse, Object> after) {
	
	}
	
	public static class KrystalServletBuilder {
		
		public KrystalServletBuilder addCountableMappings(Collection<? extends CountableMappingInterface> mappings) {
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
		
		public KrystalServletBuilder serveGetPersistenceMappings(Collection<PersistenceMappingInterface> mappings,
		                                                         Map<String, String> responseHeaders,
		                                                         @Nullable BeforeAndAfter beforeAndAfter) {
			return this.serveGet(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .setExceptionsHandler(new ExceptionsHandler(e -> log.error("ServeGetPersistence", e)))
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
						                             
						                             if (beforeAndAfter != null && beforeAndAfter.before != null) beforeAndAfter.before.accept(req, resp);
						                             AtomicReference<Object> requestResult = new AtomicReference<>();
						                             
						                             if (info.patternIsPlural) {
							                             val params = req.getParameterMap();
							                             val clazz = info.mapping.getPersistenceClass();
							                             
							                             Persistence.promiseAll(clazz, params.isEmpty() ? null : PersistenceFilters.fromParams(params))
							                                        .map(Stream::toList)
							                                        .apply(requestResult::set)
							                                        .map(JSON::fromObjects)
							                                        .map(JSONArray::toString)
							                                        .accept(result -> {
								                                        if (result.length() > 2) {
									                                        try {
										                                        resp.getWriter().write(result);
									                                        } catch (IOException e) {
										                                        log.error("ServeGetPersistence", e);
										                                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                                        }
								                                        } else {
									                                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
								                                        }
							                                        }).catchRun(e -> {
								                                        log.error("ServeGetPersistence", e);
								                                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							                                        })
							                                        .join();
						                             } else {
							                             try {
								                             val id = req.getHttpServletMapping().getMatchValue();
								                             // TODO include doc explanation for String argument constructor required for persistence
								                             var result = info.mapping.getPersistenceClass().getDeclaredConstructor(String.class).newInstance(id);
								                             if (result.noneIsNull()) resp.getWriter().write(result.toJSON().toString());
								                             else resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
								                             requestResult.set(result);
							                             } catch (NumberFormatException e) {
								                             log.debug("ServeGetPersistence", e);
								                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
							                             } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException e) {
								                             log.error("ServeGetPersistence", e);
								                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							                             }
						                             }
						                             
						                             if (resp.getStatus() < 300 && beforeAndAfter != null && beforeAndAfter.after != null) {
							                             beforeAndAfter.after.accept(req, resp, requestResult);
						                             }
					                             }));
		}
		
		public KrystalServletBuilder serveDeletePersistenceMappings(Collection<PersistenceMappingInterface> mappings,
		                                                            Map<String, String> responseHeaders,
		                                                            @Nullable BeforeAndAfter beforeAndAfter) {
			return this.serveDelete(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .setExceptionsHandler(new ExceptionsHandler(e -> log.error("ServeDeletePersistence", e)))
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
						                             
						                             if (beforeAndAfter != null && beforeAndAfter.before != null) beforeAndAfter.before.accept(req, resp);
						                             AtomicReference<Object> requestResult = new AtomicReference<>();
						                             
						                             try {
							                             val clazz = info.mapping.getPersistenceClass();
							                             
							                             if (info.patternIsPlural) {
								                             val params = req.getParameterMap();
								                             if (!params.isEmpty()) {
									                             // parameterized call
									                             requestResult.set(clazz.getDeclaredConstructor().newInstance()
									                                                    .getTable()
									                                                    .delete()
									                                                    .where(params)
									                                                    .promise()
									                                                    .catchRun(e -> {
										                                                    log.error("ServeDeletePersistence", e);
										                                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                                                    })
									                                                    .join());
								                             } else {
									                             // objects posted
									                             val str = req.getReader().lines().collect(Collectors.joining());
									                             val results = processBodyOfArray(new JSONArray(str), clazz, PersistenceInterface::delete);
									                             resp.getWriter().write(JSON.fromObjects(results).toString());
									                             requestResult.set(results);
								                             }
							                             } else {
								                             // single object by /id
								                             val obj = clazz.getDeclaredConstructor(String.class).newInstance(req.getHttpServletMapping().getMatchValue());
								                             obj.delete();
								                             resp.getWriter().write(obj.toJSON().toString());
								                             requestResult.set(obj);
							                             }
						                             } catch (NumberFormatException | JSONException | ClassCastException e) {
							                             log.debug("ServeDeletePersistence", e);
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						                             } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException e) {
							                             log.error("ServeDeletePersistence", e);
							                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						                             }
						                             
						                             if (resp.getStatus() < 300 && beforeAndAfter != null && beforeAndAfter.after != null) {
							                             beforeAndAfter.after.accept(req, resp, requestResult);
						                             }
					                             }));
		}
		
		public KrystalServletBuilder servePostPersistenceMappings(Collection<PersistenceMappingInterface> mappings,
		                                                          Map<String, String> responseHeaders,
		                                                          @Nullable BeforeAndAfter beforeAndAfter) {
			return this.servePost(
					(req, resp) -> VirtualPromise.supply(() -> new RequestInfo(req, mappings))
					                             .setExceptionsHandler(new ExceptionsHandler(e -> log.error("ServePostPersistence", e)))
					                             .monitor(promise -> RequestInfo.validate(promise, resp))
					                             .accept(info -> {
						                             // prep response
						                             prepStandardResponseWithHeaders(resp, responseHeaders);
						                             
						                             if (beforeAndAfter != null && beforeAndAfter.before != null) beforeAndAfter.before.accept(req, resp);
						                             AtomicReference<Object> requestResult = new AtomicReference<>();
						                             
						                             try {
							                             val str = req.getReader().lines().collect(Collectors.joining());
							                             val clazz = info.mapping.getPersistenceClass();
							                             
							                             if (info.patternIsPlural) {
								                             val results = processBodyOfArray(new JSONArray(str), clazz, PersistenceInterface::save);
								                             resp.getWriter().write(JSON.fromObjects(results).toString());
								                             requestResult.set(results);
							                             } else {
								                             processSingleJsonElement(new JSONObject(str), clazz, element -> {
									                             try {
										                             element.save();
										                             resp.getWriter().write(element.toJSON().toString());
										                             requestResult.set(element);
									                             } catch (Exception e) {
										                             log.error("ServePostPersistence", e);
										                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									                             }
								                             });
							                             }
						                             } catch (JSONException | ClassCastException e) {
							                             log.debug("ServePostPersistence", e);
							                             resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						                             } catch (IOException e) {
							                             log.error("ServePostPersistence", e);
							                             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						                             }
						                             
						                             if (resp.getStatus() < 300 && beforeAndAfter != null && beforeAndAfter.after != null) {
							                             beforeAndAfter.after.accept(req, resp, requestResult);
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
				info.kill();
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
	
	private boolean serveAsync(HttpServletRequest req, HttpServletResponse resp, @Nullable BiFunction<HttpServletRequest, HttpServletResponse, VirtualPromise<Void>> asyncResponseAction) {
		if (asyncResponseAction == null) return false;
		val id = UUID.randomUUID().toString();
		log.info("{}: Request received.", id);
		log.debug("{}: {} {}", id, req.getMethod(), req.getRequestURI());
		val asyncContext = req.startAsync();
		asyncContext.addListener(new AsyncListener() {
			@Override
			public void onComplete(AsyncEvent event) {
				log.info("{}: Context closed.", id);
			}
			
			@Override
			public void onTimeout(AsyncEvent event) {
				log.debug("{}: Context timeout.", id);
				asyncContext.complete();
			}
			
			@Override
			public void onError(AsyncEvent event) {
				log.error("{}: Context error listener.", id, event.getThrowable());
				asyncContext.complete();
			}
			
			@Override
			public void onStartAsync(AsyncEvent event) {
				log.debug("{}: Context started...", id);
			}
		});
		
		log.debug("{}: Context started...", id);
		val contextAction = VirtualPromise.supply(() -> asyncResponseAction.apply(req, resp))
		                                  .name("Response_Context_" + id)
		                                  .apply(ara -> ara.name("Async_Response_" + id))
		                                  .apply(ara -> log.debug(ara.getReport()))
		                                  .apply(ara -> monitorResponse(ara, resp))
		                                  .apply(ara -> log.debug("{}: Watching response...", id))
		                                  .accept(VirtualPromise::join)
		                                  .thenRun(() -> log.debug("{}: Response done.", id))
		                                  .catchRun(e -> log.error("{}: Context action error.", id, e))
		                                  .thenRun(asyncContext::complete);
		
		monitorContext(contextAction, asyncContext);
		
		return true;
	}
	
	/*
	 * Responses Monitor
	 */
	
	/**
	 * Setup responses monitor if not present.
	 */
	private static void setResponsesMonitor() {
		try {
			while (!RESPONSES.monitorLock.tryLock()) {
				Thread.sleep(100);
			}
			
			if (RESPONSES.monitor.get() == null) {
				RESPONSES.monitor.set(Thread.ofVirtual()
				                            .name("Responses Monitor")
				                            .start(KrystalServlet::monitorResponses));
				log.debug("Responses Monitor started.");
			}
			
			RESPONSES.monitorLock.unlock();
		} catch (Exception e) {
			log.fatal("Responses Monitor", e);
		}
	}
	
	/**
	 * Action done by monitoring thread. Stops loading if response times-out or is cancelled.
	 */
	private static void monitorResponses() {
		try {
			while (!RESPONSES.asyncResponses.isEmpty()) {
				RESPONSES.asyncResponses.forEach((promise, response) -> {
					try {
						if (promise.isDestroyed()) {
							RESPONSES.trash.add(promise);
							return;
						}
						
						if (promise.isComplete() || promise.isIdle() || promise.hasException()) {
							if (promise.hasException()) log.debug("Exception found in {}.", promise.getName(), promise.getException());
							RESPONSES.trash.add(promise);
							return;
						}
						
						try {
							response.getWriter();
						} catch (Exception e) {
							log.info("{} cancelled.", promise.getName(), e);
							try {
								response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
							} catch (Exception _) {
							}
							RESPONSES.trash.add(promise);
						}
					} catch (NullPointerException e) {
						log.warn("De-sync - the promise is, most likely, concurrently destroyed.", e);
						RESPONSES.trash.add(promise);
					}
				});
				
				RESPONSES.trash.forEach(r -> {
					try {
						if (!r.isDestroyed()) r.kill();
					} catch (NullPointerException _) {
					}
					RESPONSES.asyncResponses.remove(r);
				});
				RESPONSES.trash.clear();
				Thread.sleep(1000);
			}
			RESPONSES.monitor.set(null);
			log.debug("Responses Monitor finished gracefully.");
		} catch (Exception e) {
			log.error("Response Monitor Exception", e);
			RESPONSES.monitor.set(null);
			setResponsesMonitor();
		}
	}
	
	/**
	 * @see #monitorResponses()
	 */
	private static void monitorResponse(VirtualPromise<Void> action, HttpServletResponse response) {
		RESPONSES.asyncResponses.put(action, response);
		setResponsesMonitor();
	}
	
	/*
	 * Contexts Monitor
	 */
	
	/**
	 * Setup contexts monitor if not present.
	 */
	private static void setContextsMonitor() {
		try {
			while (!CONTEXTS.monitorLock.tryLock()) {
				Thread.sleep(100);
			}
			
			if (CONTEXTS.monitor.get() == null) {
				CONTEXTS.monitor.set(Thread.ofVirtual()
				                           .name("Contexts Monitor")
				                           .start(KrystalServlet::monitorContexts));
				log.debug("Contexts Monitor started.");
			}
			
			CONTEXTS.monitorLock.unlock();
		} catch (Exception e) {
			log.fatal("Contexts Monitor", e);
		}
	}
	
	/**
	 * Action done by monitoring thread.
	 */
	private static void monitorContexts() {
		try {
			while (!CONTEXTS.asyncContexts.isEmpty()) {
				CONTEXTS.asyncContexts.forEach((promise, context) -> {
					try {
						if (promise.isDestroyed()) {
							CONTEXTS.trash.add(promise);
							return;
						}
						
						if (promise.isComplete() || promise.isIdle() || promise.hasException()) {
							if (promise.hasException()) {
								log.debug("Exception found in {}.", promise.getName(), promise.getException());
								try {
									context.complete();
								} catch (Exception _) {
								}
							}
							CONTEXTS.trash.add(promise);
						}
					} catch (NullPointerException e) {
						log.warn("De-sync - the promise is, most likely, concurrently destroyed.", e);
						CONTEXTS.trash.add(promise);
					}
				});
				
				CONTEXTS.trash.forEach(c -> {
					try {
						if (!c.isDestroyed()) c.kill();
					} catch (NullPointerException _) {
					}
					CONTEXTS.asyncContexts.remove(c);
				});
				CONTEXTS.trash.clear();
				Thread.sleep(1000);
			}
			CONTEXTS.monitor.set(null);
			log.debug("Contexts Monitor finished gracefully.");
		} catch (Exception e) {
			log.error("Contexts Monitor Exception", e);
			CONTEXTS.monitor.set(null);
			setContextsMonitor();
		}
	}
	
	/**
	 * @see #monitorContexts()
	 */
	private static void monitorContext(VirtualPromise<Void> action, AsyncContext context) {
		CONTEXTS.asyncContexts.put(action, context);
		setContextsMonitor();
	}
	
	/*
	 * Reporting
	 */
	
	public static String report() {
		val report = new StringBuilder("\nAsync Responses: ").append(RESPONSES.asyncResponses.size());
		RESPONSES.asyncResponses.keySet().forEach(vp -> report.append("\n *** ")
		                                                      .append(vp.getReport())
		                                                      .append(";"));
		report.append("\nContexts: ").append(CONTEXTS.asyncContexts.size());
		CONTEXTS.asyncContexts.keySet().forEach(vp -> report.append("\n *** ")
		                                                    .append(vp.getReport())
		                                                    .append(";"));
		return report.toString();
	}
	
	/*
	 * Misc
	 */
	
	private record Responses(
			Map<VirtualPromise<Void>, HttpServletResponse> asyncResponses,
			List<VirtualPromise<Void>> trash,
			AtomicReference<Thread> monitor,
			ReentrantLock monitorLock
	) {
	
	}
	
	private record Contexts(
			Map<VirtualPromise<Void>, AsyncContext> asyncContexts,
			List<VirtualPromise<Void>> trash,
			AtomicReference<Thread> monitor,
			ReentrantLock monitorLock
	) {
	
	}
	
}