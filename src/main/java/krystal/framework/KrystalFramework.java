package krystal.framework;

import javafx.application.Application;
import krystal.framework.core.PropertiesInterface;
import krystal.framework.core.jfxApp;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import java.io.Console;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Launching from {@code main()} proposed flow:
 * <ol>
 *     <li>Set framework properties (variables, pre-loads, etc.);</li>
 *     <li>Either {@link #primaryInitialization(String...)} or any of the <i>"framePredefined"</i> methods;</li>
 *     <li>Any sequence of initiation methods, or "runLater" calls.</li>
 * </ol>
 *
 * @see #primaryInitialization(String...)
 */
@ComponentScan
@Configuration
@Log4j2
public class KrystalFramework {
	
	/**
	 * <p>Root folder to search for external resources. Address folder outside, i.e. next to the jar.</p> <br>
	 * <p>Default: empty String</p>
	 */
	private static @Getter @Setter String exposedDirPath = "";
	/**
	 * <p>Path and name of the <i>.properties</i> file used as the source of application properties.</p> <br>
	 * <p>Default: {@code application.properties}</p>
	 *
	 * @see PropertiesInterface
	 */
	private static @Getter @Setter String appPropertiesFile;
	/**
	 * <p>Dir path for database {@link ProviderInterface Providers} <i>.properties</i> files.</p> <br>
	 * <p>Default: {@link #exposedDirPath}</p>
	 */
	private static @Getter @Setter String providersPropertiesDir;
	/**
	 * <p>Path and name of the text file used as the source of external commands.</p> <br>
	 * <p>Default: {@code exposedDirPath/commander.txt}</p>
	 *
	 * @see krystal.framework.commander.CommanderInterface CommanderInterface
	 */
	private static @Getter @Setter String commanderFile;
	/**
	 * <p>Custom css styling. Use as pleased.</p> <br>
	 * <p>Default: {@code style.css}</p>
	 */
	private static @Getter @Setter String cssCustomFile;
	
	/**
	 * <p>Default delimiter to concatenate various Strings.</p> <br>
	 * <p>Default: {@code ", "}</p>
	 */
	private static @Getter @Setter String defaultDelimeter = ", ";
	/**
	 * <p>Default format to use i.e. when storing <i>LocalDateTime</i>.</p> <br>
	 * <p>Default: {@code yyyy-MM-dd HH:mm:ss}</p>
	 */
	private static @Getter @Setter DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	/**
	 * <p>Default format to use i.e. when storing <i>LocalDate</i>.</p> <br>
	 * <p>Default: {@code yyyy-MM-dd}</p>
	 */
	private static @Getter @Setter DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	/**
	 * <p>Default logging pattern to use with logs file appender (Log4j2).</p> <br>
	 * <p>Default: {@code %d{yyyy.MM.dd HH:mm:ss} %-5level: %msg%n}</p>
	 *
	 * @see LoggingWrapper
	 */
	private static @Getter @Setter String loggingPattern = "%d{yyyy.MM.dd HH:mm:ss} %-5level: %msg%n";
	/**
	 * Access Spring context here if created.
	 *
	 * @see #startSpringCore(List)
	 */
	private static @Getter ApplicationContext springContext;
	/**
	 * Access JavaFX application context here if created ({@link jfxApp}). Also holds convenient utilities.
	 *
	 * @see jfxApp
	 * @see #startJavaFX(String...)
	 */
	private static @Getter @Setter jfxApp jfxApplication;
	
	private static @Getter Console console;
	
	/**
	 * Loads default values (if not specified), app properties and args.
	 *
	 * @see #exposedDirPath
	 * @see #appPropertiesFile
	 * @see #providersPropertiesDir
	 * @see #commanderFile
	 * @see #cssCustomFile
	 * @see #defaultDelimeter
	 * @see #dateFormat
	 * @see #datetimeFormat
	 * @see #loggingPattern
	 * @see #springContext
	 * @see #jfxApplication
	 */
	public static void primaryInitialization(String... args) {
		
		if (appPropertiesFile == null) appPropertiesFile = exposedDirPath + "/application.properties";
		if (commanderFile == null) commanderFile = exposedDirPath + "/commander.txt";
		if (cssCustomFile == null) cssCustomFile = exposedDirPath + "/style.css";
		if (providersPropertiesDir == null) providersPropertiesDir = exposedDirPath;
		
		PropertiesInterface.load(appPropertiesFile, args);
		LoggingWrapper.initialize();
		log.fatal("=== App started" + (PropertiesInterface.areAny() ? " with properties: " + PropertiesInterface.printAll() : "."));
	}
	
	/**
	 * Launches JavaFX application, using basic framework implementation {@link jfxApp}. Follow-up with {@link javafx.application.Platform#runLater(Runnable) Platform.runLater()}.
	 */
	public static void startJavaFX(String... args) {
		CompletableFuture.runAsync(() -> Application.launch(jfxApp.class, args));
	}
	
	/**
	 * Launches Spring Annotation context builder within roots of given Classes.
	 *
	 * @see #springContext
	 */
	public static void startSpringCore(List<Class<?>> contextRootClasses) {
		val classes = new ArrayList<>(contextRootClasses);
		classes.addFirst(KrystalFramework.class);
		springContext = new AnnotationConfigApplicationContext(classes.toArray(Class[]::new));
	}
	
	public static void startTomcatServer(int port) {
		val baseDir = System.getProperty("java.io.tmpdir");
		val handler = WebHttpHandlerBuilder.applicationContext(springContext).build();
		val servlet = new TomcatHttpHandlerAdapter(handler);
		val tomcat = new Tomcat();
		
		val rootContext = tomcat.addContext("", Path.of(baseDir).toAbsolutePath().toString());
		Tomcat.addServlet(rootContext, "main", servlet).setAsyncSupported(true);
		rootContext.addServletMappingDecoded("/", "main");
		
		tomcat.setHostname("localhost");
		tomcat.setPort(port);
		tomcat.setBaseDir(baseDir);
		
		try {
			tomcat.start();
		} catch (LifecycleException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * <p>Frame JavaFX application backed by Spring annotation context.</p>
	 */
	public static void frameSpringJavaFX(List<Class<?>> springContextRootClasses, String... args) {
		primaryInitialization(args);
		startJavaFX(args);
		startSpringCore(springContextRootClasses);
	}
	
	public static void quit() {
		log.fatal("=== Clean Exit");
		System.exit(0);
	}
	
}