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
	exit, loglvl, log, console, cls, clear, props, providers, help, krystal, spring, tomcat
	// TODO restart, commanderOff, reload, etc
}