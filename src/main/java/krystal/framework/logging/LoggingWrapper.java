package krystal.framework.logging;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.core.PropertiesAndArguments;
import krystal.framework.core.PropertiesInterface;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.stereotype.Service;

/**
 * Simple logging wrapper around log4j. Use LoggingInterface to attach log() method to classes.
 *
 * @see LoggingInterface
 */
@Service
public class LoggingWrapper {
	
	// logger instance
	public static final Logger ROOT_LOGGER = (Logger) LogManager.getRootLogger();
	// custom test level
	public static final Level TEST = Level.forName("TEST", 700);
	
	// set the default logfile name
	private static final String DEFAULT_LOGFILE = "logfile";
	// set the default logfile location
	private static final String DEFAULT_LOGDIR =
			Tools.concatAsURIPath(KrystalFramework.getExposedDirPath(), "logs");
	
	private LoggingWrapper() {
		
		// Set level to given from command line or FATAL as default (also if level is not parsed correctly)
		setRootLevel((String) PropertiesAndArguments.loglvl.value().orElse("all"));
		
		// Start file appender if not suspended by cmdArgs
		if ((Boolean) PropertiesAndArguments.logtofile.value().orElse(true))
			try {
				startFileAppender();
			} catch (IllegalStateException ex) {
				ROOT_LOGGER.fatal("=== Logging to file - failed initiation. Check directory access.");
			}
		
	}
	
	public static LoggingWrapper getInstance() {
		return KrystalFramework.getSpringContext().getBean(LoggingWrapper.class);
	}
	
	/**
	 * Set logger level with string parsing.
	 */
	public void setRootLevel(String level) {
		Configurator.setRootLevel(parseLogLevel(level));
	}
	
	/**
	 * Use this method to get logger level from String values (i.e. cmdLineArgs) (Implements try-catch)
	 */
	private Level parseLogLevel(String level) {
		try {
			return Level.valueOf(level);
		} catch (NullPointerException | IllegalArgumentException ex) {
			return Level.FATAL;
		}
	}
	
	private void startFileAppender() {
		String logfile =
				Tools.concatAsURIPath(
						(String) PropertiesAndArguments.logdir.value().orElse(DEFAULT_LOGDIR),
						(String) PropertiesAndArguments.logfile.value().orElse(DEFAULT_LOGFILE)
				);
		
		var rfa = RollingFileAppender
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
		
		ROOT_LOGGER.addAppender(rfa);
		rfa.start();
		ROOT_LOGGER.fatal("=== Logging to file started at level " + ROOT_LOGGER.getLevel() + (PropertiesInterface.areAny() ? ", with App properties: " + PropertiesInterface.printAll() : "."));
	}
	
}