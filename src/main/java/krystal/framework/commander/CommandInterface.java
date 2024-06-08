package krystal.framework.commander;

import krystal.framework.commander.implementation.BaseCommander;

/**
 * Command used by {@link CommanderInterface} or {@link BaseCommander}.
 */
@FunctionalInterface
public interface CommandInterface {
	
	String name();
	
}