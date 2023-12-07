package framework.core.flow;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record Task(String name, CompletableFuture<?> task, FlowInterface flow) {
	
	public String report() {
		return String.format("Task [%s], flow: [%s] status: %s.", name, flow, getStatus());
	}
	
	public String getStatus() {
		if (task.isCompletedExceptionally()) return "Done with exceptions";
		if (task.isDone()) return "Done";
		if (task.isCompletedExceptionally()) return "Cancelled with exceptions";
		if (task.isCancelled()) return "Cancelled";
		return "Ongoing";
	}
	
	public boolean isDone() {
		return task.isDone();
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		Task task = (Task) object;
		return Objects.equals(name, task.name) && Objects.equals(flow, task.flow);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, flow);
	}
	
	@Override
	public String toString() {
		return report();
	}
	
}