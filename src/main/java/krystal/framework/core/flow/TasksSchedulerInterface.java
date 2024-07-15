package krystal.framework.core.flow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@FunctionalInterface
public interface TasksSchedulerInterface {
	
	ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(1);
	
	Map<ScheduledTask, ScheduledFuture<?>> getScheduledTasks();
	
	default void startSchedule(ScheduledTask scheduledTask, Runnable taskRunnable, long interval, TimeUnit unit) {
		cancelSchedule(scheduledTask);
		getScheduledTasks().put(scheduledTask, SCHEDULER.scheduleAtFixedRate(taskRunnable, 0, interval, unit));
	}
	
	default void cancelSchedule(ScheduledTask scheduledAction) {
		Optional.ofNullable(getScheduledTasks().remove(scheduledAction))
		        .ifPresent(action -> action.cancel(true));
	}
	
	@FunctionalInterface
	interface ScheduledTask {
		
		String name();
		
		default boolean equals(ScheduledTask other) {
			return this.name().equals(other.name());
		}
		
	}
	
}