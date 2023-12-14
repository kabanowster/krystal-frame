package krystal.framework.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface LoggingInterface {
	
	default Logger log() {
		return LogManager.getLogger();
	}
	
	default void logTest(String message) {
		LoggingWrapper.ROOT_LOGGER.log(LoggingWrapper.TEST, message);
	}
	
}