package krystal.framework.logging;

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
	
	/**
	 * Can be used in non-static context.
	 */
	default Logger log() {
		return LogManager.getLogger();
	}
	
	/**
	 * Uses custom TEST level of 700.
	 */
	default void logTest(String message) {
		LoggingWrapper.ROOT_LOGGER.log(LoggingWrapper.TEST, message);
	}
	
	/**
	 * Uses custom CONSOLE level of 800.
	 */
	default void logConsole(String message) {
		LoggingWrapper.ROOT_LOGGER.log(LoggingWrapper.CONSOLE, message);
	}
	
}