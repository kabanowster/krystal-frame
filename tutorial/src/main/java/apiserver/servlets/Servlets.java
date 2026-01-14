package apiserver.servlets;

import apiserver.database.Providers;
import jakarta.servlet.http.HttpServletResponse;
import krystal.VirtualPromise;
import krystal.framework.database.implementation.Q;
import krystal.framework.tomcat.KrystalServlet;
import krystal.framework.tomcat.PersistenceMappingInterface;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
@Log4j2
public class Servlets {
	
	private final String CONTEXT_BASE = "/api/v1";
	
	public KrystalServlet statisticsServlet() {
		return defaultServlet("Statistics", "/statistics", StatMappings.values());
	}
	
	public KrystalServlet defaultServlet(String name, String context, PersistenceMappingInterface[] mappings) {
		return KrystalServlet.getPersistenceServlet(CONTEXT_BASE + context, name, Stream.of(mappings).collect(Collectors.toSet()), "*");
	}
	
	public KrystalServlet customCommentsServlet() {
		val origin = Map.of(
				"Access-Control-Allow-Origin", "*",
				"Access-Control-Allow-Credentials", "true"
		);
		val options = Map.of(
				"Access-Control-Allow-Origin", "*",
				"Access-Control-Allow-Methods", "POST, DELETE, OPTIONS",
				"Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization"
		);
		
		return KrystalServlet.builder()
		                     .servletName("Custom Comments")
		                     .servletContextString(CONTEXT_BASE + "/secondary")
		                     .mapping("/doComments")
		                     .serveOptions((req, resp) ->
				                                   VirtualPromise.run(() -> {
					                                   options.forEach(resp::setHeader);
					                                   resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
				                                   }))
		                     .servePost((req, resp) -> VirtualPromise.run(() -> {
			                     origin.forEach(resp::setHeader);
			                     try {
				                     for (var obj : new JSONArray(req.getReader().lines().collect(Collectors.joining()))) {
					                     val comment = (JSONObject) obj;
					                     Q.q(String.format("CALL comments.comin ('%s', %s, '%s')",
					                                       comment.getString("stamp").replace('T', ' '),
					                                       comment.get("referenceId"),
					                                       comment.get("comment")
					                      ))
					                      .setProvider(Providers.secondary)
					                      .promise()
					                      .join();
				                     }
				                     
				                     resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			                     } catch (IOException | JSONException e) {
				                     log.error("ServePostComment", e);
				                     resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			                     }
		                     }))
		                     .serveDelete((req, resp) -> VirtualPromise.run(() -> {
			                     origin.forEach(resp::setHeader);
			                     try {
				                     for (var obj : new JSONArray(req.getReader().lines().collect(Collectors.joining()))) {
					                     val comment = (JSONObject) obj;
					                     Q.q(String.format("EXEC comments.comrem (%s)", comment.get("id")))
					                      .setProvider(Providers.primary)
					                      .promise()
					                      .join();
				                     }
				                     
				                     resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
			                     } catch (IOException | JSONException e) {
				                     log.error("ServeDeleteComment", e);
				                     resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			                     }
		                     }))
		                     .build();
	}
	
}