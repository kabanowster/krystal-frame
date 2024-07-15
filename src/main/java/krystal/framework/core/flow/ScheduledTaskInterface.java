package krystal.framework.core.flow;

@FunctionalInterface
public interface ScheduledTaskInterface {
	
	String name();
	
	default boolean equals(ScheduledTaskInterface other) {
		return this.name().equals(other.name());
	}
	
}