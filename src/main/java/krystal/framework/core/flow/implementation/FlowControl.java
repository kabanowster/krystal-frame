package krystal.framework.core.flow.implementation;

import krystal.framework.core.flow.FlowControlInterface;
import krystal.framework.core.flow.FlowInterface;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Service
@Getter
public class FlowControl implements FlowControlInterface {
	
	private final Map<FlowInterface, Phaser> flowControls;
	private final ScheduledExecutorService scheduledExecutor;
	
	private FlowControl() {
		flowControls = new ConcurrentHashMap<>();
		scheduledExecutor = new ScheduledThreadPoolExecutor(1);
		initialize(Flows.values());
	}
	
}