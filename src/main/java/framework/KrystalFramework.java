package framework;

import framework.core.JavaFXApplication;
import framework.core.PropertyInterface;
import javafx.application.Application;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Log4j2
@UtilityClass
public class KrystalFramework {
	
	/**
	 * Root folder to search for resources. String path with ".\your-folder" to address folder outside, next to the jar.
	 */
	private @Getter @Setter String exposedDirPath = "";
	/**
	 * Path and name of the text file used as the source of application properties.
	 */
	private @Getter @Setter String appPropertiesFile = exposedDirPath + "application.properties";
	/**
	 * Path and name of the text file used as the source of external commands.
	 */
	private @Getter @Setter String commanderFile = exposedDirPath + "commander.comms";
	/**
	 * Custom css styling.
	 */
	private @Getter @Setter String cssCustomFile = exposedDirPath + "style.css";
	
	/**
	 * Default delimeter to concatenate various Strings.
	 */
	private @Getter @Setter String defaultDelimeter = ", ";
	/**
	 * Default format to use when storing <i>LocalDateTime</i>.
	 */
	private @Getter @Setter DateTimeFormatter datetimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	/**
	 * Default format to use when storing <i>LocalDate</i>.
	 */
	private @Getter @Setter DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private @Getter @Setter String loggingPattern = "%highlight{%d{yyyy.MM.dd HH:mm:ss.SSS} %-5level: %msg [%t]%n}{STYLE=Logback}";
	
	private @Getter ApplicationContext springContext;
	private @Getter @Setter JavaFXApplication javaFXApplication;
	
	/**
	 * Loads arguments, properties and Spring annotation configuration context.
	 */
	public void primaryInitialisation(String... args) {
		PropertyInterface.load(appPropertiesFile, args);
		log.fatal("=== App started" + (PropertyInterface.areAny() ? " with properties: " + PropertyInterface.printAll() : ""));
	}
	
	/**
	 * Launches JavaFX application, using basic framework implementation. Follow-up with {@link javafx.application.Platform#runLater(Runnable) Platform.runLater()}.
	 */
	public void startJavaFX(String[] args) {
		CompletableFuture.runAsync(() -> Application.launch(JavaFXApplication.class, args));
	}
	
	public void startSpringCore(Class<?> contextRootClass) {
		springContext = new AnnotationConfigApplicationContext(FrameworkRoot.class, contextRootClass);
	}
	
	public void quit() {
		log.fatal("=== Clean Exit");
		System.exit(0);
	}
	
}