package krystal.framework.core.flow;

import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;

public interface FlowControlInterface extends LoggingInterface {
	
	Map<FlowInterface, Phaser> getFlowControls();
	
	ScheduledExecutorService getScheduledExecutor();
	
	static FlowControlInterface getInstance() {
		try {
			return KrystalFramework.getSpringContext().getBean(FlowControlInterface.class);
		} catch (NullPointerException e) {
			return null;
		} catch (NoSuchBeanDefinitionException e) {
			LoggingWrapper.ROOT_LOGGER.fatal(e.getMessage());
			return null;
		}
	}
	
	default void initialize(FlowInterface... flows) {
		log().debug("*** Initializing Flows.");
		for (FlowInterface s : flows)
			getFlowControls().put(s, new Phaser(1));
	}
	
}