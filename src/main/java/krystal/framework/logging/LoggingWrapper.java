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

/**
 * Simple logging wrapper around log4j. Use {@link LoggingInterface} to attach {@link LoggingInterface#log() log()} method to classes. Call {@link #initialize()} to load properties from {@link PropertiesInterface} and start start file appender if the
 * {@link PropertiesAndArguments#logtofile logtofile} property is true.
 * <p>Use properties:</p>
 * <dl>
 *     <dt><b><i>logtofile</i></b></dt>
 *     <dd>Determines if the file appender should be loaded during {@link #initialize()};</dd>
 *     <dt><b><i>logfile</i></b></dt>
 *     <dd>Sets the name of the file. You can set this property dynamically via accessing {@link PropertiesInterface#properties} directly;</dd>
 *     <dt><b><i>logdir</i></b></dt>
 *     <dd>Sets the path to where the logfiles should be saved. Can also be set programmatically.</dd>
 * </dl>
 *
 * @see LoggingInterface
 * @see #startFileAppender()
 */
@UtilityClass
public class LoggingWrapper {
	
	// logger instance
	public static final Logger ROOT_LOGGER = (Logger) LogManager.getRootLogger();
	
	/**
	 * Custom TEST level. Use by invoking {@link LoggingInterface#logTest(String)}.
	 */
	public static final Level TEST = Level.forName("TEST", 700);
	
	/**
	 * Custom CONSOLE level. Use by invoking {@link LoggingInterface#logConsole(String)}.
	 */
	public static final Level CONSOLE = Level.forName("CONSOLE", 1);
	
	// set the default logfile name
	private static final String DEFAULT_LOGFILE = "logfile";
	// set the default logfile location
	private static final String DEFAULT_LOGDIR =
			Tools.concatAsURIPath(KrystalFramework.getExposedDirPath(), "logs");
	
	public static RollingFileAppender fileAppender;
	
	public void initialize() {
		
		// Set level to given from command line or FATAL as default (also if level is not parsed correctly)
		setRootLevel((String) PropertiesAndArguments.loglvl.value().orElse("all"));
		
		// Start file appender if not suspended by cmdArgs
		if ((Boolean) PropertiesAndArguments.logtofile.value().orElse(false))
			try {
				startFileAppender();
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
	public static Level parseLogLevel(String level) {
		try {
			return Level.valueOf(level);
		} catch (NullPointerException | IllegalArgumentException ex) {
			return Level.ALL;
		}
	}
	
	/**
	 * Builds and (re)starts default {@link RollingFileAppender} including loaded {@link PropertiesAndArguments} - <i>logfile</i> and <i>logdir</i>.
	 */
	private void startFileAppender() {
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
				               .setFileName(logfile + ".log")
				               .setName("RollingFile")
				               .setFilePattern(logfile + "-%i.log")
				               .setLayout(
						               PatternLayout.newBuilder()
						                            .setPattern(KrystalFramework.getLoggingPattern())
						                            .build())
				               .setPolicy(
						               SizeBasedTriggeringPolicy.createPolicy("5MB"))
				               .setStrategy(
						               DefaultRolloverStrategy.newBuilder()
						                                      .setMax("3")
						                                      .build())
				               .build();
		
		ROOT_LOGGER.addAppender(fileAppender);
		fileAppender.start();
		ROOT_LOGGER.fatal("=== Logging to file started at level {}{}", ROOT_LOGGER.getLevel(), PropertiesInterface.areAny() ? ", with App properties: " + PropertiesInterface.printAll() : ".");
	}
	
}