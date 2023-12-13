package framework.core.flow;

import framework.KrystalFramework;
import framework.logging.LoggingInterface;
import lombok.val;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public interface FlowControlInterface extends LoggingInterface {
	
	static FlowControlInterface getInstance() {
		return KrystalFramework.getSpringContext().getBean(FlowControlInterface.class);
	}
	
	Map<FlowInterface, Phaser> getFlowControls();
	
	ScheduledExecutorService getScheduledExecutor();
	
	ExecutorService getCachedExecutor();
	
	List<Task> getTasksList();
	
	default void initializeTaskManager(FlowInterface... flows) {
		log().debug("*** Initializing Tasks Manager.");
		for (FlowInterface s : flows)
			getFlowControls().put(s, new Phaser(1));
		getScheduledExecutor().scheduleWithFixedDelay(this::removeCompletedTasks, 3, 1, TimeUnit.SECONDS);
	}
	
	/**
	 * Use to synchronise tasks execution between different, unrelated objects / threads, through {@link FlowInterface}.
	 */
	default void registerTask(String name, CompletableFuture<?> task, FlowInterface flow) {
		val toRegister = new Task(name, task, flow);
		
		// prevent overflow
		if (getTasksList().contains(toRegister))
			return;
		
		getFlowControls().get(flow).register();
		getTasksList().add(toRegister);
		log().debug(String.format("*** [Task Manager] Registered %s Total: %s", toRegister, getTasksList().size()));
	}
	
	default void deregisterTask(String name) {
		deregisterTasks(getTasksList().stream().filter(t -> name != null ? name.equals(t.name()) : t.name() == null).toList());
	}
	
	default void deregisterTasks(List<Task> tasks) {
		tasks.forEach(t -> {
			t.task().cancel(true);
			t.flow().arriveAndDeregister();
		});
		getTasksList().removeAll(tasks);
		log().trace("*** [Tasks Manager] Deregistered tasks: " + tasks.size());
	}
	
	default void cancelAllTasks() {
		deregisterTasks(getTasksList());
	}
	
	default void removeCompletedTasks() {
		var completedTasks = getTasksList().stream().filter(Task::isDone).toList();
		if (!completedTasks.isEmpty()) {
			log().debug(String.format("*** [Tasks Manager] Completed %s tasks.", completedTasks.size()));
			deregisterTasks(completedTasks);
			reportTasks(completedTasks);
		}
	}
	
	default void reportTasks(List<Task> tasks) {
		tasks.forEach(t -> {
			log().trace(t.report());
			t.task().handle((result, ex) -> {
				if (result != null) log().trace("  > Task result: " + result);
				if (ex != null) {
					log().debug(String.format("  ! [%s] Exception: %s", t.name(), ex.getMessage()));
					ex.printStackTrace();
				}
				return t;
			});
		});
	}
	
}