package krystal.framework.core.flow;

import krystal.VirtualPromise;

import java.util.Objects;

public record Task(String name, VirtualPromise<?> task, FlowInterface flow) {
	
	public String report() {
		return String.format("Task [%s], flow: [%s] status: %s.", name, flow, getStatus());
	}
	
	public String getStatus() {
		if (task.isComplete() && task.getException() != null) return "Done with exceptions";
		if (task.isComplete()) return "Done";
		if (task.getHoldState().get()) return "On hold.";
		return "Ongoing";
	}
	
	public boolean isDone() {
		return task.isComplete();
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