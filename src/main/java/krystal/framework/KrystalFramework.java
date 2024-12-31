package krystal.framework;

import javafx.application.Application;
import krystal.ConsoleView;
import krystal.JSON;
import krystal.framework.commander.CommanderInterface;
import krystal.framework.core.NativeConsoleReader;
import krystal.framework.core.PropertiesAndArguments;
import krystal.framework.core.PropertiesInterface;
import krystal.framework.core.flow.FlowControlInterface;
import krystal.framework.core.jfxApp;
import krystal.framework.database.abstraction.ConnectionPoolInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.implementation.DefaultProviders;
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
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Launching from {@code main()} proposed flow:
 * <ol>
 *     <li>Set framework properties (variables, pre-loads, etc.);</li>
 *     <li>Either {@link #primaryInitialization(String...)} or any of the <i>"framePredefined"</i> methods;</li>
 *     <li>Any sequence of initiation methods, or "runLater" calls.</li>
 * </ol>
 *
 * @see #primaryInitialization(String...)
 * @see #frameSpringConsole(String, List, String...)
 * @see #frameSpringJavaFX(List, String...)
 * @see #startTomcatServer(TomcatProperties)
 */
@Log4j2
@UtilityClass
public class KrystalFramework {
	
	/*
	 * Settings variables
	 */
	
	/**
	 * <p>Root folder to search for external resources. </p> <br>
	 * <p>Default: empty String</p>
	 *
	 * @implNote Address folder outside, i.e. next to the jar.
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
	 * <p>Dir path for database {@link ProviderInterface} <i>.properties</i> files.</p> <br>
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
	 * Select default database {@link ProviderInterface Provider}. If not set, the {@link DefaultProviders#sqlserver} will be used.
	 * Add your own providers to the pool setting or modifying {@link #setProvidersPool(List)}.
	 */
	private @Getter @Setter ProviderInterface defaultProvider = DefaultProviders.sqlserver;
	/**
	 * The pool of database providers used to load them with default {@link krystal.framework.database.implementation.QueryExecutor QueryExecutor} or by name references (i.e. app properties or args).
	 */
	private @Getter @Setter List<ProviderInterface> providersPool = Arrays.stream(DefaultProviders.values()).collect(Collectors.toCollection(ArrayList::new));
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
	/**
	 * Simple Swing console-log view.
	 *
	 * @see #startConsole()
	 * @see #disposeConsole()
	 * @see ConsoleView
	 */
	private @Getter ConsoleView console;
	/**
	 * Accessor for framework's Tomcat running implementation.
	 *
	 * @see #startTomcatServer(TomcatProperties)
	 * @see TomcatProperties
	 * @see TomcatFactory
	 */
	private @Getter Tomcat tomcat;
	/**
	 * Splash string to output upon initialization.
	 */
	private @Getter @Setter @Nullable String initializationSplash = getDefaultSplash();
	/*
	 * Launch modules
	 */
	
	/**
	 * Loads framework settings values, app properties and args.
	 *
	 * @implNote Set settings values before invoking this method. Run this method before loading other modules.
	 * @see #setExposedDirPath(String)
	 * @see #setAppPropertiesFile(String)
	 * @see #setProvidersPropertiesDir(String)
	 * @see #setCommanderFile(String)
	 * @see #setCssCustomFile(String)
	 * @see #setDefaultDelimeter(String)
	 * @see #setDateFormat(DateTimeFormatter)
	 * @see #setDatetimeFormat(DateTimeFormatter)
	 * @see #setLoggingPattern(String)
	 * @see #setDefaultProvider(ProviderInterface)
	 * @see #setProvidersPool(List)
	 * @see #selectDefaultImplementations(DefaultImplementation...)
	 * @see #selectAllDefaultImplementationsExcept(DefaultImplementation...)
	 * @see #setInitializationSplash(String)
	 * @see #getSpringContext()
	 * @see #getJfxApplication()
	 * @see #getTomcat()
	 */
	public void primaryInitialization(String... args) {
		Optional.ofNullable(initializationSplash).ifPresent(log::fatal);
		
		// not defined on declaration because of dependency on exposedDirPath
		if (appPropertiesFile == null) appPropertiesFile = exposedDirPath + "/application.properties";
		if (commanderFile == null) commanderFile = exposedDirPath + "/commander.txt";
		if (cssCustomFile == null) cssCustomFile = exposedDirPath + "/style.css";
		if (providersPropertiesDir == null) providersPropertiesDir = exposedDirPath;
		
		PropertiesInterface.load(appPropertiesFile, args);
		LoggingWrapper.initialize();
		
		// load jdbc drivers
		try {
			Class.forName("com.ibm.as400.access.AS400JDBCDriver");
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			Class.forName("org.h2.Driver");
			Class.forName("org.postgresql.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			log.fatal(e.getMessage());
		}
		
		// default provider from properties / args
		PropertiesAndArguments
				.provider.value()
				         .flatMap(
						         p -> providersPool
								              .stream()
								              .filter(pp -> p.equals(pp.name()))
								              .findFirst())
				         .ifPresent(dp -> defaultProvider = dp);
		
		log.fatal("=== App started" + (PropertiesInterface.areAny() ? " with properties: " + PropertiesInterface.printAll() : "."));
	}
	
	/**
	 * Launches JavaFX application, using basic framework implementation {@link jfxApp}. Follow-up with {@link javafx.application.Platform#runLater(Runnable) Platform.runLater()}.
	 */
	public void startJavaFX(String... args) {
		// only platform thread, not virtual
		Thread.ofPlatform().start(() -> Application.launch(jfxApp.class, args));
	}
	
	/**
	 * Launches Spring Annotation context builder within roots of given Classes.
	 *
	 * @see #springContext
	 */
	public void startSpringCore(List<Class<?>> contextRootClasses) {
		val classes = new ArrayList<>(contextRootClasses);
		// classes.addFirst(KrystalFramework.class);
		if (selectedDefaultImplementations == null)
			selectDefaultImplementations();
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
		} catch (LifecycleException e) {
			log.fatal("!!! Tomcat broke with exception:\n" + e.getMessage());
		}
		log.fatal("  > Tomcat is running on: %1$s:%2$s".formatted(properties.getHostName(), properties.getPort()));
	}
	
	/**
	 * (Re)starts the Swing console-log view.
	 *
	 * @see ConsoleView
	 */
	public void startConsole(@Nullable String windowTitle) {
		disposeConsole();
		console = new ConsoleView(
				windowTitle,
				LoggingWrapper.ROOT_LOGGER,
				PatternLayout.newBuilder()
				             .setPattern(loggingPattern)
				             .build(),
				command -> CommanderInterface.getInstance().ifPresent(ci -> ci.parseCommand(command))
		);
	}
	
	/**
	 * @see #startConsole(String)
	 */
	public void startConsole() {
		startConsole(null);
	}
	
	/**
	 * Destroys the Swing console-log view.
	 *
	 * @see ConsoleView
	 */
	public void disposeConsole() {
		if (console != null)
			console.dispose();
		console = null;
	}
	
	/**
	 * @return {@link NativeConsoleReader} loaded into Spring context.
	 * @see #selectDefaultImplementations(DefaultImplementation...)
	 * @see #startSpringCore(List)
	 */
	public Optional<NativeConsoleReader> getNativeConsoleReader() {
		try {
			return Optional.of(springContext.getBean(NativeConsoleReader.class));
		} catch (BeansException e) {
			return Optional.empty();
		}
	}
	
	/*
	 * Launch templates
	 */
	
	/**
	 * Frame JavaFX application, backed by Spring annotation context.
	 */
	public void frameSpringJavaFX(List<Class<?>> springContextRootClasses, String... args) {
		primaryInitialization(args);
		startJavaFX(args);
		startSpringCore(springContextRootClasses);
	}
	
	/**
	 * Frame simple Swing console-log output, backed by Spring annotation context.
	 */
	public void frameSpringConsole(@Nullable String consoleWindowTitle, List<Class<?>> springContextRootClasses, String... args) {
		startConsole(consoleWindowTitle);
		primaryInitialization(args);
		startSpringCore(springContextRootClasses);
	}
	
	public void frameSpringConsole(List<Class<?>> springContextRootClasses, String... args) {
		frameSpringConsole(null, springContextRootClasses, args);
	}
	/*
	 * Utilities
	 */
	
	public void quit() {
		log.fatal("=== Clean Exit");
		System.exit(0);
	}
	
	/**
	 * Choose the {@link DefaultImplementation default implementations} of core framework components, to be loaded during Spring component scan.
	 *
	 * @param selectedImplementations
	 * 		If none, all default implementations will be loaded.
	 */
	public void selectDefaultImplementations(DefaultImplementation... selectedImplementations) {
		selectedDefaultImplementations = Set.of(selectedImplementations.length == 0 ? DefaultImplementation.values() : selectedImplementations);
	}
	
	/**
	 * Choose all {@link DefaultImplementation default implementations} of core framework components, except selected, to be loaded during Spring component scan.
	 *
	 * @param excludedImplementations
	 * 		If none, all default implementations will be loaded.
	 */
	public void selectAllDefaultImplementationsExcept(DefaultImplementation... excludedImplementations) {
		selectedDefaultImplementations = new HashSet<>(Set.of(DefaultImplementation.values()));
		if (excludedImplementations.length > 0)
			selectedDefaultImplementations.removeAll(Set.of(excludedImplementations));
	}
	
	/**
	 * List of default implementations available for core interfaces in the framework.
	 *
	 * @see #selectDefaultImplementations(DefaultImplementation...)
	 * @see #selectAllDefaultImplementationsExcept(DefaultImplementation...)
	 * @see QueryExecutorInterface
	 * @see FlowControlInterface
	 * @see CommanderInterface
	 * @see ConnectionPoolInterface
	 * @see NativeConsoleReader
	 * @see krystal.framework.database.persistence.PersistenceMemory PersistenceMemory
	 */
	public enum DefaultImplementation {
		FlowControl(krystal.framework.core.flow.implementation.FlowControl.class),
		QueryExecutor(krystal.framework.database.implementation.QueryExecutor.class),
		BaseCommander(krystal.framework.commander.implementation.BaseCommander.class),
		ConnectionPool(krystal.framework.database.implementation.ConnectionPool.class),
		NativeConsole(NativeConsoleReader.class),
		PersistenceMemory(krystal.framework.database.persistence.PersistenceMemory.class);
		
		public final Class<?> implementation;
		
		DefaultImplementation(Class<?> clazz) {
			implementation = clazz;
		}
	}
	
	/**
	 * Output framework settings variables values in JSON format.
	 */
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
		
		return JSON.fromObject(map).toString(4);
	}
	
	private String getDefaultSplash() {
		//@formatter:off
		return """


 /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\\s
//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\
\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//
 \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/\s
 /\\                                                                                  /\\\s
//\\\\        __ __                 __        __   ______                             //\\\\
\\\\//       / //_/_______  _______/ /_____ _/ /  / ____/________ _____ ___  ___      \\\\//
 \\/       / ,<  / ___/ / / / ___/ __/ __ `/ /  / /_  / ___/ __ `/ __ `__ \\/ _ \\      \\/\s
 /\\      / /| |/ /  / /_/ (__  ) /_/ /_/ / /  / __/ / /  / /_/ / / / / / /  __/      /\\\s
//\\\\    /_/ |_/_/   \\__, /____/\\__/\\__,_/_/  /_/   /_/   \\__,_/_/ /_/ /_/\\___/      //\\\\
\\\\//               /____/                                                           \\\\//
 \\/                                                                                  \\/\s
 /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\  /\\\s
//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\
\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//\\\\//
 \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/  \\/
 
 				          """;
		//@formatter:on
	}
	
}