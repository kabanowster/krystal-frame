package krystal.framework.tomcat;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.catalina.startup.Tomcat;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import java.io.File;
import java.util.Optional;

/**
 * Use {@link #buildServer(TomcatProperties)} to create embedded {@link Tomcat} implementation.
 *
 * @see TomcatProperties
 * @see KrystalHttpServlet
 */
@Log4j2
@UtilityClass
public class TomcatFactory {
	
	/**
	 * Default {@link Tomcat} implementation. Sets-up basic properties, attaches web-app if provided, adds default context with spring web-handlers (i.e. {@link org.springframework.web.bind.annotation.RestController @RestController}s), along
	 * with custom {@link KrystalHttpServlet}s.
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
		 * API context pattern: server:port/contextPath/servletMapping/springControllersMappings
		 */
		
		// default context
		val context = tomcat.addContext(properties.getServletsRootContext(), baseDir.getAbsolutePath());
		
		// Spring controllers
		try {
			val handler = WebHttpHandlerBuilder.applicationContext(KrystalFramework.getSpringContext()).build();
			val servlet = new TomcatHttpHandlerAdapter(handler);
			Tomcat.addServlet(context, "spring", servlet).setAsyncSupported(true);
			context.addServletMappingDecoded("/", "spring");
		} catch (NoSuchBeanDefinitionException e) {
			log.fatal("  ! Web Handlers are missing within Spring context (like @RestController). Skipped Spring servlet.");
		}
		
		// Custom servlets
		properties.getServlets().forEach(servlet -> {
			val name = servlet.getServletName();
			Tomcat.addServlet(context, name, servlet);
			
			if (servlet instanceof KrystalHttpServlet s) {
				if (s.getMappings().isEmpty()) {
					log.fatal("  ! No mappings are defined for %s servlet.".formatted(name));
				} else {
					s.getMappings().forEach(m -> context.addServletMappingDecoded(m, name));
				}
			} else {
				// jakarta compatible servlets TODO: testing
				context.addServletMappingDecoded(servlet.getServletContext().getContextPath(), servlet.getServletName());
			}
		});
		
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