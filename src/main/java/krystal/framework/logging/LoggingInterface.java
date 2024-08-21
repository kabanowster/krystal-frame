package krystal.framework.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convenient way to access {@link Logger} from interfaces.
 *
 * @see #log()
 * @see #logTest(String)
 * @see #logConsole(String)
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
	}
	
	/**
	 * Uses custom CONSOLE level of 1.
	 */
	default void logConsole(String message) {
		log().log(LoggingWrapper.CONSOLE, message);
	}
	
}