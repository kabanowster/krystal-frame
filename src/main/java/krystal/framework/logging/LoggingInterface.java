package krystal.framework.logging;

import lombok.val;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convenient way to access {@link Logger}.
 *
 * @see #log()
 * @see #logTest(String)
 * @see LoggingWrapper
 */
public interface LoggingInterface {
	
	static Logger logger() {
		return LogManager.getLogger();
	}
	
	/**
	 * Can be used in non-static context.
	 */
	default Logger log() {
		return logger();
	}
	
	/**
	 * Uses custom TEST level of 700.
	 */
	default void logTest(String message) {
		log().log(LoggingWrapper.TEST, message);
		// LoggingWrapper.ROOT_LOGGER.log(LoggingWrapper.TEST, message);
	}
	
	/**
	 * Uses custom CONSOLE level of 1.
	 */
	default void logConsole(String message) {
		log().log(LoggingWrapper.CONSOLE, message);
	}
	
	default RuntimeException logFatalAndThrow(String message) {
		return logFatalAndThrow(logger(), message);
	}
	
	default RuntimeException logFatalAndThrow(Throwable throwable) {
		return logFatalAndThrow(logger(), throwable);
	}
	
	static RuntimeException logFatalAndThrow(Logger logger, String message) {
		val exception = new RuntimeException(message);
		logger.fatal(exception.getMessage());
		return exception;
	}
	
	static RuntimeException logFatalAndThrow(Logger logger, Throwable throwable) {
		val exception = new RuntimeException(throwable);
		logger.fatal(exception.getMessage());
		return exception;
	}
	
}