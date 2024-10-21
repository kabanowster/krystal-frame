package krystal.framework.commander.implementation;

import krystal.ConsoleView;
import krystal.framework.commander.CommandInterface;
import krystal.framework.commander.CommanderInterface;

/**
 * List of parsable basic framework commands.
 *
 * @see CommanderInterface
 * @see ConsoleView ConsoleView
 */
public enum BaseCommands implements CommandInterface {
	help, cls, log, props, exit, console, providers, krystal, spring, tomcat, pmem
	// TODO restart, commanderOff, reload, etc
}