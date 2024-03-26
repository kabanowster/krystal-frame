package krystal.framework;

import javafx.application.Application;
import krystal.JSON;
import krystal.framework.core.ConsoleView;
import krystal.framework.core.PropertiesInterface;
import krystal.framework.core.jfxApp;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.logging.LoggingWrapper;
import krystal.framework.tomcat.TomcatFactory;
import krystal.framework.tomcat.TomcatProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.format.DateTimeFormatter;
import java.util.*;
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
@Log4j2
@UtilityClass
public class KrystalFramework {
	
	/*
	 * Settings variables
	 */
	
	/**
	 * <p>Root folder to search for external resources. Address folder outside, i.e. next to the jar.</p> <br>
	 * <p>Default: empty String</p>
	 */
	private @Getter @Setter String exposedDirPath = "";
	/**
	 * <p>Path and name of the <i>.properties</i> file used as the source of application properties.</p> <br>
	 * <p>Default: {@code application.properties}</p>
	 *
	 * @see PropertiesInterface
	 */
	private @Getter @Setter String appPropertiesFile;
	/**
	 * <p>Dir path for database {@link ProviderInterface Providers} <i>.properties</i> files.</p> <br>
	 * <p>Default: {@link #exposedDirPath}</p>
	 */
	private @Getter @Setter String providersPropertiesDir;
	/**
	 * <p>Path and name of the text file used as the source of external commands.</p> <br>
	 * <p>Default: {@code exposedDirPath/commander.txt}</p>
	 *
	 * @see krystal.framework.commander.CommanderInterface CommanderInterface
	 */
	private @Getter @Setter String commanderFile;
	/**
	 * <p>Custom css styling. Use as pleased.</p> <br>
	 * <p>Default: {@code style.css}</p>
	 */
	private @Getter @Setter String cssCustomFile;
	
	/**
	 * <p>Default delimiter to concatenate various Strings.</p> <br>
	 * <p>Default: {@code ", "}</p>
	 */
	private @Getter @Setter String defaultDelimeter = ", ";
	/**
	 * <p>Default format to use i.e. when storing <i>LocalDateTime</i>.</p> <br>
	 * <p>Default: {@code yyyy-MM-dd HH:mm:ss}</p>
	 */
	private @Getter @Setter DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	/**
	 * <p>Default format to use i.e. when storing <i>LocalDate</i>.</p> <br>
	 * <p>Default: {@code yyyy-MM-dd}</p>
	 */
	private @Getter @Setter DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	/**
	 * <p>Default logging pattern to use with logs file appender (Log4j2).</p> <br>
	 * <p>Default: {@code %d{yyyy.MM.dd HH:mm:ss} %-5level: %msg%n}</p>
	 *
	 * @see LoggingWrapper
	 */
	private @Getter @Setter String loggingPattern = "%d{yyyy.MM.dd HH:mm:ss} %-7level: %msg%n";
	/**
	 * Access Spring context here if created.
	 *
	 * @see #startSpringCore(List)
	 */
	private @Getter ApplicationContext springContext;
	/**
	 * Access JavaFX application context here if created ({@link jfxApp}). Also holds convenient utilities.
	 *
	 * @see jfxApp
	 * @see #startJavaFX(String...)
	 */
	private @Getter @Setter jfxApp jfxApplication;
	
	/**
	 * @see #selectDefaultImplementations(DefaultImplementation...)
	 */
	private @Getter Set<DefaultImplementation> selectedDefaultImplementations;
	
	private @Getter ConsoleView console;
	
	private @Getter Tomcat tomcat;
	
	/*
	 * Launch modules
	 */
	
	/**
	 * Loads default values (if not specified), app properties and args.
	 *
	 * @see #setExposedDirPath(String)
	 * @see #setAppPropertiesFile(String)
	 * @see #setProvidersPropertiesDir(String)
	 * @see #setCommanderFile(String)
	 * @see #setCssCustomFile(String)
	 * @see #setDefaultDelimeter(String)
	 * @see #setDateFormat(DateTimeFormatter)
	 * @see #setDatetimeFormat(DateTimeFormatter)
	 * @see #setLoggingPattern(String)
	 * @see #getSpringContext()
	 * @see #getJfxApplication()
	 */
	public void primaryInitialization(String... args) {
		
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
	public void startJavaFX(String... args) {
		CompletableFuture.runAsync(() -> Application.launch(jfxApp.class, args));
	}
	
	/**
	 * Launches Spring Annotation context builder within roots of given Classes.
	 *
	 * @see #springContext
	 */
	public void startSpringCore(List<Class<?>> contextRootClasses) {
		val classes = new ArrayList<>(contextRootClasses);
		// classes.addFirst(KrystalFramework.class);
		classes.addAll(selectedDefaultImplementations.stream().map(i -> i.implementation).toList());
		springContext = new AnnotationConfigApplicationContext(classes.toArray(Class[]::new));
	}
	
	/**
	 * Starts new embedded {@link Tomcat} server with provided {@link TomcatProperties}.
	 *
	 * @see TomcatFactory
	 * @see TomcatProperties
	 */
	public void startTomcatServer(TomcatProperties properties) {
		log.fatal("=== Initializing Tomcat Server...");
		try {
			tomcat = TomcatFactory.buildServer(properties);
			tomcat.start();
			tomcat.getConnector();
		} catch (LifecycleException e) {
			log.fatal("!!! Tomcat broke with exception:\n" + e.getMessage());
		}
		log.fatal("  > Tomcat is running on: %1$s:%2$s".formatted(properties.getHostName(), properties.getPort()));
	}
	
	public void startConsole() {
		disposeConsole();
		console = new ConsoleView();
	}
	
	public void disposeConsole() {
		if (console != null)
			console.dispose();
		console = null;
	}
	
	/*
	 * Launch templates
	 */
	
	/**
	 * Frame JavaFX application backed by Spring annotation context.
	 */
	public void frameSpringJavaFX(List<Class<?>> springContextRootClasses, String... args) {
		primaryInitialization(args);
		startJavaFX(args);
		startSpringCore(springContextRootClasses);
	}
	
	public void frameSpringConsole(List<Class<?>> springContextRootClasses, String... args) {
		startConsole();
		primaryInitialization(args);
		startSpringCore(springContextRootClasses);
	}
	
	// public void frameSpringWebConsole(Class<?> mainClass, String... args) {
	// 	startConsole();
	// 	primaryInitialization(args);
	// 	startSpringWebCore(List.of(mainClass));
	// 	startTomcatServer(null, 8082, mainClass);
	// }
	
	/*
	 * Utilities
	 */
	
	public void quit() {
		log.fatal("=== Clean Exit");
		System.exit(0);
	}
	
	/**
	 * Choose the default implementations of core framework components, to be loaded during Spring component scan.
	 *
	 * @param selectedImplementations
	 * 		If Null, all default implementations will be loaded.
	 */
	public void selectDefaultImplementations(DefaultImplementation... selectedImplementations) {
		selectedDefaultImplementations = new HashSet<>(Set.of(selectedImplementations.length == 0 ? DefaultImplementation.values() : selectedImplementations));
	}
	
	/**
	 * Choose all default implementations of core framework components, except selected, to be loaded during Spring component scan.
	 *
	 * @param excludedImplementations
	 * 		If Null, all default implementations will be loaded.
	 */
	public void selectAllDefaultImplementationsExcept(DefaultImplementation... excludedImplementations) {
		selectedDefaultImplementations = new HashSet<>(Set.of(DefaultImplementation.values()));
		if (excludedImplementations.length > 0)
			selectedDefaultImplementations.removeAll(Set.of(excludedImplementations));
	}
	
	public enum DefaultImplementation {
		FlowControl(krystal.framework.core.flow.implementation.FlowControl.class),
		QueryExecutor(krystal.framework.database.implementation.QueryExecutor.class),
		BaseCommander(krystal.framework.commander.implementation.BaseCommander.class);
		
		public final Class<?> implementation;
		
		DefaultImplementation(Class<?> clazz) {
			implementation = clazz;
		}
	}
	
	public String frameworkStatus() {
		val map = new HashMap<>();
		
		map.put("exposedDirPath", exposedDirPath);
		map.put("appPropertiesFile", appPropertiesFile);
		map.put("providersPropertiesDir", providersPropertiesDir);
		map.put("commanderFile", commanderFile);
		map.put("cssCustomFile", cssCustomFile);
		map.put("defaultDelimeter", defaultDelimeter);
		map.put("datetimeFormat", datetimeFormat.toString());
		map.put("dateFormat", dateFormat.toString());
		map.put("loggingPattern", loggingPattern);
		map.put("springContext", springContext != null ? "established" : "n/a");
		map.put("jfxApplication", jfxApplication != null ? "established" : "n/a");
		map.put("tomcat", tomcat != null ? "established" : "n/a");
		map.put("console", console != null ? "established" : "n/a");
		map.put("selectedDefaultImplementations", selectedDefaultImplementations.stream().map(Enum::toString).toList());
		
		return JSON.from(map).toString(4);
	}
	
}