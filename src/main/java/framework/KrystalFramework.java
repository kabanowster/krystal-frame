package framework;

import framework.core.PropertyInterface;
import framework.core.jfxApp;
import javafx.application.Application;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@ComponentScan
@Configuration
@Log4j2
public class KrystalFramework {
	
	/**
	 * Root folder to search for resources. String path with "\your-folder" to address folder outside, next to the jar.
	 */
	private static @Getter @Setter String exposedDirPath = "";
	/**
	 * Path and name of the text file used as the source of application properties.
	 */
	private static @Getter @Setter String appPropertiesFile;
	
	private static @Getter @Setter String providersPropertiesDir;
	
	/**
	 * Path and name of the text file used as the source of external commands.
	 */
	private static @Getter @Setter String commanderFile;
	/**
	 * Custom css styling.
	 */
	private static @Getter @Setter String cssCustomFile;
	
	/**
	 * Default delimeter to concatenate various Strings.
	 */
	private static @Getter @Setter String defaultDelimeter = ", ";
	/**
	 * Default format to use when storing <i>LocalDateTime</i>.
	 */
	private static @Getter @Setter DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	/**
	 * Default format to use when storing <i>LocalDate</i>.
	 */
	private static @Getter @Setter DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private static @Getter @Setter String loggingPattern = "%highlight{%d{yyyy.MM.dd HH:mm:ss.SSS} %-5level: %msg [%t]%n}{STYLE=Logback}";
	
	private static @Getter ApplicationContext springContext;
	private static @Getter @Setter jfxApp jfxApplication;
	
	/**
	 * Loads arguments, properties and Spring annotation configuration context.
	 */
	public static void primaryInitialization(String... args) {
		if (appPropertiesFile == null) appPropertiesFile = exposedDirPath + "/application.properties";
		if (commanderFile == null) commanderFile = exposedDirPath + "/commander.txt";
		if (cssCustomFile == null) cssCustomFile = exposedDirPath + "/style.css";
		if (providersPropertiesDir == null) providersPropertiesDir = exposedDirPath;
		
		PropertyInterface.load(appPropertiesFile, args);
		log.fatal("=== App started" + (PropertyInterface.areAny() ? " with properties: " + PropertyInterface.printAll() : ""));
	}
	
	/**
	 * Launches JavaFX application, using basic framework implementation. Follow-up with {@link javafx.application.Platform#runLater(Runnable) Platform.runLater()}.
	 */
	public static void startJavaFX(String... args) {
		CompletableFuture.runAsync(() -> Application.launch(jfxApp.class, args));
	}
	
	public static void startSpringCore(Class<?> contextRootClass) {
		springContext = new AnnotationConfigApplicationContext(KrystalFramework.class, contextRootClass);
	}
	
	/**
	 * Frame and launch JavaFX application backed with Spring annotation context.
	 */
	public static void frameSpringJavaFX(Class<?> springContextRootClass, String... args) {
		primaryInitialization(args);
		startJavaFX(args);
		startSpringCore(springContextRootClass);
	}
	
	public static void quit() {
		log.fatal("=== Clean Exit");
		System.exit(0);
	}
	
}