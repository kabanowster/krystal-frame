package krystal.framework.logging;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.core.PropertiesAndArguments;
import krystal.framework.core.PropertiesInterface;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.Optional;

/**
 * Simple logging wrapper around log4j. Use {@link LoggingInterface} to attach {@link LoggingInterface#log() log()} method to classes. Call {@link #initialize()} to load properties from {@link PropertiesInterface} and start file appender if the
 * {@link PropertiesAndArguments#logtofile logtofile} property is true.
 * <p>Use properties:</p>
 * <dl>
 *     <dt><b><i>logtofile</i></b></dt>
 *     <dd>Determines if the file appender should be loaded during {@link #initialize()};</dd>
 *     <dt><b><i>logfile</i></b></dt>
 *     <dd>Sets the name of the file. You can set this property programmatically via accessing {@link PropertiesInterface#properties} directly;</dd>
 *     <dt><b><i>logdir</i></b></dt>
 *     <dd>Sets the path to where the logfiles should be saved. Can also be set programmatically.</dd>
 * </dl>
 * Additional log levels: {@link #TEST}, {@link #CONSOLE}
 *
 * @see LoggingInterface
 * @see #initFileAppender()
 * @see #setRootLevel(String)
 * @see #parseLogLevel(String)
 */
@UtilityClass
public class LoggingWrapper {
	
	// logger instance
	public final Logger ROOT_LOGGER = (Logger) LogManager.getRootLogger();
	
	/**
	 * Custom TEST level. Use by invoking {@link LoggingInterface#logTest(String)}.
	 */
	public final Level TEST = Level.forName("TEST", 700);
	
	/**
	 * Custom CONSOLE level. Use by invoking {@link LoggingInterface#logConsole(String)}.
	 */
	public final Level CONSOLE = Level.forName("CONSOLE", 1);
	
	private final String DEFAULT_LOGFILE = "logfile";
	private final String DEFAULT_LOGDIR = Tools.concatAsURIPath(KrystalFramework.getExposedDirPath(), "logs");
	
	public RollingFileAppender fileAppender;
	
	public void initialize() {
		
		// Set level to given from command line or FATAL as default (also if level is not parsed correctly)
		setRootLevel((String) PropertiesAndArguments.loglvl.value().orElse("fatal"));
		
		// Start file appender if not suspended by cmdArgs
		if ((Boolean) PropertiesAndArguments.logtofile.value().orElse(false))
			try {
				initFileAppender();
			} catch (IllegalStateException ex) {
				ROOT_LOGGER.fatal("=== Logging to file - failed initiation. Check directory access.");
			}
		
	}
	
	/**
	 * Set logger level with string parsing.
	 */
	public void setRootLevel(String level) {
		Configurator.setRootLevel(parseLogLevel(level));
	}
	
	/**
	 * Use this method to get log4j2 {@link Level} from String values.
	 *
	 * @return {@link Level} or {@link Level#ALL} if fails parsing.
	 */
	public Level parseLogLevel(String level) {
		try {
			return Level.valueOf(level);
		} catch (NullPointerException | IllegalArgumentException ex) {
			return Level.ALL;
		}
	}
	
	/**
	 * Builds and (re)starts default {@link RollingFileAppender} including loaded {@link PropertiesAndArguments} - <i>logfile</i> and <i>logdir</i>.
	 */
	public void initFileAppender() {
		if (fileAppender != null) {
			fileAppender.stop();
			ROOT_LOGGER.removeAppender(fileAppender);
			ROOT_LOGGER.fatal("=== File logs appender will be replaced.");
		}
		
		String logfile =
				Tools.concatAsURIPath(
						(String) PropertiesAndArguments.logdir.value().orElse(DEFAULT_LOGDIR),
						(String) PropertiesAndArguments.logfile.value().orElse(DEFAULT_LOGFILE)
				);
		
		fileAppender = RollingFileAppender
				               .newBuilder()
				               .withFileName(logfile + ".log")
				               .setName("RollingFile")
				               .withFilePattern(logfile + "-%i.log")
				               .setLayout(
						               PatternLayout.newBuilder()
						                            .withPattern(KrystalFramework.getLoggingPattern())
						                            .build())
				               .withPolicy(
						               SizeBasedTriggeringPolicy.createPolicy("5MB"))
				               .withStrategy(
						               DefaultRolloverStrategy.newBuilder()
						                                      .withMax("3")
						                                      .build())
				               .build();
		
		ROOT_LOGGER.addAppender(fileAppender);
		fileAppender.start();
		ROOT_LOGGER.fatal("=== Logging to file started at level {}, to file: {}", ROOT_LOGGER.getLevel(), fileAppender.getFileName());
	}
	
	public void stopFileAppender() {
		Optional.ofNullable(fileAppender).ifPresentOrElse(
				rfa -> {
					rfa.stop();
					ROOT_LOGGER.warn("=== Logging to file stopped. Use <b><i>start</i></b> command to bring it back.");
				},
				() -> ROOT_LOGGER.warn("=== Logging to file can not be stopped, because file appender was not initialized. Use 'log -fa init' to initialize it first."));
	}
	
	public void startFileAppender() {
		Optional.ofNullable(fileAppender).ifPresentOrElse(
				rfa -> {
					rfa.start();
					ROOT_LOGGER.warn("=== Logging to file started.");
				},
				() -> ROOT_LOGGER.warn("=== Logging to file can not be started, because file appender was not initialized. Use 'log -fa init' to initialize it first."));
	}
	
}