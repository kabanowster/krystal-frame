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
	
	Map<ScheduledTaskInterface, ScheduledFuture<?>> getScheduledTasks();
	
	default void startSchedule(ScheduledTaskInterface scheduledTask, Runnable taskRunnable, long interval, TimeUnit unit) {
		cancelSchedule(scheduledTask);
		getScheduledTasks().put(scheduledTask, SCHEDULER.scheduleAtFixedRate(taskRunnable, 0, interval, unit));
	}
	
	default void cancelSchedule(ScheduledTaskInterface scheduledAction) {
		Optional.ofNullable(getScheduledTasks().remove(scheduledAction))
		        .ifPresent(action -> action.cancel(true));
	}
	
}