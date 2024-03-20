package krystal.framework.core.flow.implementation;

import krystal.framework.core.flow.FlowControlInterface;
import krystal.framework.core.flow.FlowInterface;
import krystal.framework.core.flow.Task;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

//@Service
@Getter
public class FlowControl implements FlowControlInterface {
	
	private final ScheduledExecutorService scheduledExecutor;
	private final ExecutorService cachedExecutor;
	private final List<Task> tasksList;
	private final Map<FlowInterface, Phaser> flowControls;
	private final AtomicReference<ScheduledFuture<?>> taskManager;
	
	private FlowControl() {
		scheduledExecutor = Executors.newScheduledThreadPool(3);
		cachedExecutor = Executors.newCachedThreadPool();
		tasksList = Collections.synchronizedList(new ArrayList<>());
		flowControls = Collections.synchronizedMap(new HashMap<>());
		taskManager = new AtomicReference<>();
		
		initialize(true, Flows.values());
	}
	
}