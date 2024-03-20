package krystal.framework.commander.implementation;

import krystal.framework.commander.CommandInterface;
import krystal.framework.commander.CommanderInterface;

/**
 * List of parsable basic framework commands.
 *
 * @see CommanderInterface
 * @see krystal.framework.core.ConsoleView ConsoleView
 */
public enum BaseCommands implements CommandInterface {
	exit, loglvl, log, console, cls, clear, props, providers, help, krystal, spring
	// TODO restart, commanderOff, reload, etc
}