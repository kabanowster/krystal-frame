package krystal.framework.tomcat;

import krystal.Tools;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;

/**
 * Use {@link #buildServer(TomcatProperties)} to create embedded {@link Tomcat} implementation.
 *
 * @see TomcatProperties
 * @see KrystalServlet
 */
@Log4j2
@UtilityClass
public class TomcatFactory {
	// TODO allow properties from server.properties file
	
	/**
	 * Default {@link Tomcat} implementation. Sets-up basic properties, attaches web-app if provided, with custom {@link KrystalServlet}s.
	 */
	public Tomcat buildServer(TomcatProperties properties) {
		val tomcat = new Tomcat();
		
		// root dir
		val baseDir = new File(properties.getBaseDir());
		if (baseDir.mkdirs()) log.trace("    New Tomcat root directory created: " + baseDir.getAbsolutePath());
		
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		tomcat.setHostname(properties.getHostName());
		tomcat.setPort(properties.getPort());
		
		/*
		 * Copy or set Web-App if .war / dir provided
		 */
		
		// create root dir for web-apps if missing and load current apps if present
		var webAppsDir = properties.getWebappsDir();
		tomcat.getHost().setAppBase(webAppsDir);
		webAppsDir = Tools.concatAsURIPath(baseDir.getAbsolutePath(), webAppsDir);
		val webApps = new File(webAppsDir);
		if (webApps.mkdirs()) {
			log.trace("    New Tomcat web-apps root directory created: " + webApps.getAbsolutePath());
		} else {
			log.trace("    Loading web-apps from current root...");
			/*
			 * folder is present, try to autoload apps from it
			 */
			addAppsFromDir(tomcat, webApps);
		}
		
		// process sources passed in properties
		Optional.ofNullable(properties.getAppSrc()).ifPresent(
				src -> Tools.getResource(src).ifPresentOrElse(
						u -> {
							if (properties.isAppSrcAsCollection()) {
								addAppsFromDir(tomcat, src);
							} else {
								addApp(tomcat, properties.getAppName(), src);
							}
						},
						() -> log.fatal("    Provided appSrc not found."))
		);
		
		/*
		 * API context pattern: server:port/contextPath/servletMapping
		 */
		
		// For each servlet - filter for context (KrystalServlet can have it defined), or default. Create contexts with servlets.
		
		// Tomcat has no method to list contexts?
		val contexts = new HashSet<Context>();
		val defaultContextString = properties.getDefaultServletsContext();
		
		// Custom servlets
		properties.getServlets().forEach(servlet -> {
			val name = Optional.ofNullable(servlet.getServletName()).orElse("unnamedServlet");
			val contextString = servlet instanceof KrystalServlet s ? Optional.ofNullable(s.getServletContextString()).orElse(defaultContextString) : defaultContextString;
			
			val context = contexts.stream()
			                      .filter(c -> c.getPath().equals(contextString))
			                      .findFirst()
			                      .orElseGet(() -> {
				                      val result = tomcat.addContext(contextString, baseDir.getAbsolutePath());
				                      contexts.add(result);
				                      return result;
			                      });
			
			val wrapper = Tomcat.addServlet(context, name, servlet);
			
			if (servlet instanceof KrystalServlet s) {
				if (s.getMappings().isEmpty()) {
					log.fatal("  ! No mappings are defined for %s<%s> servlet. Adding default of '/'.".formatted(name, servlet.getClass().getSimpleName()));
					context.addServletMappingDecoded("/", name);
				} else {
					s.getMappings().forEach(m -> context.addServletMappingDecoded(m, name));
				}
				wrapper.setAsyncSupported(true);
			} else {
				// other jakarta compatible servlets
				context.addServletMappingDecoded(servlet.getServletContext().getContextPath(), servlet.getServletName());
			}
		});
		
		// creates default Connector in tomcat
		val connector = tomcat.getConnector();
		
		val connectionTimeout = properties.getConnectionTimeout();
		if (connectionTimeout > 0) connector.setAsyncTimeout(connectionTimeout);
		
		// additional properties
		Optional.ofNullable(properties.getConnectorSettings()).ifPresent(settings -> settings.accept(connector));
		
		return tomcat;
	}
	
	public void addApp(Tomcat tomcat, String appName, String appSrc) {
		appName = Tools.concatAsURIPath(appName);
		appName = "/" + appName.replaceAll("[\\s/]", "_");
		log.fatal("    Adding [%s] web-app into context, from: %s".formatted(appName, appSrc));
		tomcat.addWebapp(appName, appSrc);
	}
	
	public void addAppsFromDir(Tomcat tomcat, File srcDir) {
		Optional.ofNullable(srcDir.listFiles())
		        .ifPresent(l -> {
			                   for (var dir : l) {
				                   var name = dir.getName();
				                   if (dir.isDirectory()) {
					                   addApp(tomcat, name, dir.getAbsolutePath());
				                   } else {
					                   if (!name.endsWith(".war")) continue;
					                   name = name.split("\\.(?=\\w+?$)", 2)[0];
					                   addApp(tomcat, name, dir.getAbsolutePath());
				                   }
			                   }
		                   }
		        );
	}
	
	public void addAppsFromDir(Tomcat tomcat, String srcDirPath) {
		addAppsFromDir(tomcat, new File(srcDirPath));
	}
	
}