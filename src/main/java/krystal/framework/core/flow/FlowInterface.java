package krystal.framework.core.flow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.stream.IntStream;

@FunctionalInterface
public interface FlowInterface {
	
	String name();
	
	private FlowControlInterface flowController() {
		return FlowControlInterface.getInstance();
	}
	
	private Phaser controller() {
		return flowController().getFlowControls().get(this);
	}
	
	default void register() {
		controller().register();
	}
	
	default void register(int times) {
		IntStream.range(0, times).forEach(i -> controller().register());
	}
	
	default void await(int phase) {
		controller().awaitAdvance(phase);
	}
	
	default void arriveAndAwait() {
		controller().arriveAndAwaitAdvance();
	}
	
	default void arriveAndDeregister() {
		controller().arriveAndDeregister();
	}
	
	default void deregisterAwait() {
		controller().awaitAdvance(controller().arriveAndDeregister());
	}
	
	default void arrive() {
		controller().arrive();
	}
	
	default void registerTask(String taskName, CompletableFuture<?> task) {
		flowController().registerTask(taskName, task, this);
	}
	
}