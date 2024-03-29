package krystal.framework.core.flow;

import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public interface FlowControlInterface extends LoggingInterface {
	
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
	
	Map<FlowInterface, Phaser> getFlowControls();
	
	ScheduledExecutorService getScheduledExecutor();
	
	ExecutorService getExecutor();
	
	AtomicReference<ScheduledFuture<?>> getTaskManager();
	
	List<Task> getTasksList();
	
	default void initialize(boolean withTaskManager, FlowInterface... flows) {
		log().debug("*** Initializing Tasks Manager and Flows.");
		for (FlowInterface s : flows)
			getFlowControls().put(s, new Phaser(1));
		val taskManager = getTaskManager();
		if (withTaskManager) {
			if (taskManager.get() == null)
				taskManager.set(getScheduledExecutor().scheduleWithFixedDelay(this::removeCompletedTasks, 3, 1, TimeUnit.SECONDS));
			else log().debug("    Task Manager is currently running. No need to create another.");
		}
	}
	
	/**
	 * Use to synchronise tasks execution between different, unrelated objects / threads, through {@link FlowInterface}. Tasks are being cleared by watching thread upon completion.
	 */
	default void registerTask(Task toRegister) {
		// prevent overflow
		if (getTasksList().contains(toRegister))
			return;
		
		getFlowControls().get(toRegister.flow()).register();
		getTasksList().add(toRegister);
		log().debug(String.format("*** [Task Manager] Registered %s Total: %s", toRegister, getTasksList().size()));
	}
	
	default void registerTask(String name, CompletableFuture<?> task, FlowInterface flow) {
		registerTask(new Task(name, task, flow));
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