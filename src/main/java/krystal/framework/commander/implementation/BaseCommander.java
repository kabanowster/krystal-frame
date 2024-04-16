package krystal.framework.commander.implementation;

import krystal.JSON;
import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.commander.CommandInterface;
import krystal.framework.commander.CommanderInterface;
import krystal.framework.core.ConsoleView;
import krystal.framework.core.PropertiesInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.logging.LoggingWrapper;
import krystal.framework.tomcat.TomcatFactory;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.catalina.LifecycleException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class to handle various basic commands. You can construct your own Commander extending this class and while overriding {@link #executeCommand(CommandInterface, List)} method, invoking it's superclass variant.
 *
 * @see CommanderInterface
 */
@Service
@NoArgsConstructor
public class BaseCommander implements CommanderInterface {
	
	@Override
	public boolean executeCommand(CommandInterface command, List<String> arguments) {
		
		BaseCommands acceptedCommand;
		try {
			acceptedCommand = BaseCommands.valueOf(command.name());
		} catch (IllegalArgumentException e) {
			log().fatal("  ! Command unidentified.");
			return false;
		}
		
		switch (acceptedCommand) {
			case exit -> {
				log().fatal(">>> EXIT COMMAND");
				if (!arguments.isEmpty()) {
					getValueIfArgumentIs(arguments.getFirst(), "-m", "msg").ifPresent(
							s -> {
								log().fatal("--> WITH MESSAGE: " + s);
							}
					);
				}
				KrystalFramework.quit();
				return true;
			}
			case loglvl -> {
				logConsole("--> Set logger level");
				LoggingWrapper.setRootLevel(arguments.isEmpty() ? null : arguments.getFirst());
				logConsole("    New level: " + log().getLevel().name());
				return true;
			}
			case log -> {
				if (arguments.isEmpty()) return false;
				var lvl = LoggingWrapper.CONSOLE;
				String msg = null;
				
				for (var arg : arguments) {
					if (argumentMatches(arg, "-m", "msg")) {
						msg = getArgumetnValue(arg);
						continue;
					}
					
					if (argumentMatches(arg, "-l", "lvl")) {
						lvl = LoggingWrapper.parseLogLevel(getArgumetnValue(arg));
						continue;
					}
					
					msg = arg;
				}
				
				if (msg == null) return false;
				
				log().log(lvl, msg);
				return true;
			}
			case console -> {
				KrystalFramework.startConsole();
				return true;
			}
			case cls, clear -> {
				Optional.ofNullable(KrystalFramework.getConsole()).ifPresent(ConsoleView::clear);
				return true;
			}
			case props -> {
				for (var arg : arguments) {
					if (argumentMatches(arg, "-l", "list")) {
						logConsole(">>> Stored Properties: " + PropertiesInterface.printAll());
						return true;
					}
					
					if (argumentMatches(arg, "-r", "remove")) {
						val prop = getArgumetnValue(arg);
						PropertiesInterface.properties.remove(prop);
						logConsole(">>> Property removed: \"%s\".".formatted(prop));
						return true;
					}
					
					val props = arg.split(" ", 2);
					val prop = props[0];
					
					switch (props.length) {
						case 1 -> {
							if (PropertiesInterface.properties.containsKey(prop)) {
								logConsole(">>> Current value for property \"%s\": %s".formatted(prop, PropertiesInterface.properties.get(prop)));
							} else {
								logConsole(">>> Property \"%s\" is not set.".formatted(prop));
							}
							return true;
						}
						case 2 -> {
							PropertiesInterface.properties.put(prop, props[1]);
							logConsole(">>> Set value for property \"%s\": %s".formatted(prop, props[1]));
							return true;
						}
						default -> {
							return false;
						}
					}
				}
				
				logConsole("??? Specify the arguments of the props command: -l/--list to list all properties, -r/--remove to remove a property, [name] to return property or [name] [value] to set.");
				return false;
			}
			case providers -> {
				QueryExecutorInterface.getInstance().ifPresent(q -> {
					logConsole(">>> Loaded DefaultProviders properties:\n%s".formatted(JSON.from(q.getConnectionProperties()).toString(4)));
				});
				return true;
			}
			case help -> {
				logConsole(">>> List of basic commands: %s".formatted(
						Arrays.stream(BaseCommands.values()).map(Enum::toString).collect(Collectors.joining(KrystalFramework.getDefaultDelimeter()))
				));
				return true;
			}
			case spring -> {
				Optional.ofNullable(KrystalFramework.getSpringContext()).ifPresent(c -> {
					logConsole(">>> Loaded %s Spring Beans:\n\n%s".formatted(
							c.getBeanDefinitionCount(),
							String.join("\n", c.getBeanDefinitionNames())
					));
				});
				return true;
			}
			case krystal -> {
				logConsole(">>> KrystalFramework status:\n%s".formatted(KrystalFramework.frameworkStatus()));
				return true;
			}
			case tomcat -> {
				val tomcat = KrystalFramework.getTomcat();
				if (tomcat == null) {
					logConsole(">>> Tomcat server is not running.");
					return true;
				}
				
				try {
					for (var arg : arguments) {
						if (argumentMatches(arg, "-s", "stop")) {
							tomcat.stop();
							logConsole(">>> Tomcat stopped.");
							return true;
						}
						
						if (argumentMatches(arg, "-i", "start")) {
							tomcat.start();
							tomcat.getConnector();
							logConsole(">>> Tomcat started.");
							return true;
						}
						
						if (argumentMatches(arg, "-r", "restart")) {
							tomcat.stop();
							tomcat.start();
							tomcat.getConnector();
							logConsole(">>> Tomcat restarted.");
							return true;
						}
						
						if (argumentMatches(arg, "-a", "app")) {
							var app = getArgumetnValue(arg).split("\\s", 2);
							if (app.length != 2) {
								logConsole(">>> Tomcat --app command takes exactly 2 arguments separated with space: appName and appSrc.");
								return false;
							}
							
							for (var a : app) {
								if (a.isEmpty()) {
									logConsole(">>> Tomcat --app arguments must not be empty.");
									return false;
								}
							}
							
							if (Tools.getResource(app[1]).isEmpty()) {
								logConsole(">>> Provided appSrc not found.");
								return false;
							}
							
							TomcatFactory.addApp(tomcat, app[0], app[1]);
						}
					}
				} catch (LifecycleException e) {
					log().fatal("!!! Tomcat broke with exception:\n" + e.getMessage());
				}
				
				return true;
			}
			default -> {
				return false;
			}
		}
	}
	
}