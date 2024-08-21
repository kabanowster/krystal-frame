package krystal.framework.commander.implementation;

import com.google.common.base.Strings;
import krystal.ConsoleView;
import krystal.JSON;
import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.commander.CommandInterface;
import krystal.framework.commander.CommanderInterface;
import krystal.framework.core.PropertiesInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.logging.LoggingWrapper;
import krystal.framework.tomcat.TomcatFactory;
import lombok.val;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Class to handle various basic commands. You can construct your own Commander extending this class and while overriding {@link #executeCommand(CommandInterface, List)} method, invoking its superclass variant if you want to keep these basics.
 *
 * @see CommanderInterface
 */
@Service
public class BaseCommander implements CommanderInterface {
	
	@Override
	public boolean executeCommand(CommandInterface command, List<String> arguments) {
		
		logConsole(">>>>%s [%s]".formatted(command.name(), String.join(" ", arguments)));
		
		BaseCommands acceptedCommand;
		try {
			acceptedCommand = BaseCommands.valueOf(command.name());
		} catch (IllegalArgumentException e) {
			return false;
		}
		
		switch (acceptedCommand) {
			case exit -> {
				log().fatal(">>> EXIT COMMAND");
				if (!arguments.isEmpty()) {
					CommanderInterface.getValueIfArgumentMatches(arguments.getFirst(), "m", "msg").ifPresent(s -> log().fatal("--> WITH MESSAGE: {}", s));
				}
				KrystalFramework.quit();
				return true;
			}
			case log -> {
				if (arguments.isEmpty()) {
					logConsole(">>> Current logger level: " + log().getLevel().name());
					return true;
				}
				
				String lvlArg = null;
				boolean print = false;
				val printArgs = new ArrayList<String>();
				for (var arg : arguments) {
					if (CommanderInterface.argumentMatches(arg, "?", "h", "help")) {
						logConsole("""
						           <h2>log arguments</h2>
						           <dl>
						           	<dt><b><i>lvl</i></b> new_level</dt><dd>Set a new root logger level.</dd>
						           	<dt><b><i>print</i></b> [args]</dt><dd>
						           		Print to logging stream. Use "-m", "-msg", "--" or put your message in quotes. Use "-l" argument to set a level for the message, CONSOLE by default.
						           	</dd>
						           </dl>
						           """);
						return true;
					}
				}
				for (var arg : arguments) {
					if (CommanderInterface.argumentMatches(arg, "fa")) {
						Optional.ofNullable(CommanderInterface.getArgumentValue(arg)).ifPresentOrElse(
								fa -> {
									switch (fa) {
										case "init" -> LoggingWrapper.initFileAppender();
										case "start" -> LoggingWrapper.startFileAppender();
										case "stop" -> LoggingWrapper.stopFileAppender();
										default -> logConsole(">>> Unidentified logging file appender command.");
									}
								},
								() -> logConsole(">>> Logging to file status: %s.".formatted(
										Optional.ofNullable(LoggingWrapper.fileAppender)
										        .map(AbstractLifeCycle::getState)
										        .map(Enum::name)
										        .map(n -> "%s. Target file: %s".formatted(n, LoggingWrapper.fileAppender.getFileName()))
										        .orElse("not initialized")))
						);
						continue;
					}
					
					if (CommanderInterface.argumentMatches(arg, "lvl")) {
						lvlArg = arg;
						continue;
					}
					
					if (CommanderInterface.argumentMatches(arg, "print")) {
						print = true;
					} else {
						printArgs.add(arg);
					}
				}
				
				if (lvlArg != null) {
					LoggingWrapper.setRootLevel(CommanderInterface.getArgumentValue(lvlArg));
					logConsole(">>> New logger level: " + log().getLevel().name());
				}
				
				if (print) {
					var lvl = new AtomicReference<>(LoggingWrapper.CONSOLE);
					val msg = new StringBuilder();
					for (var arg : printArgs) {
						CommanderInterface.getValueIfArgumentMatches(arg, "", "m", "msg").ifPresent(msg::append);
						CommanderInterface.getValueIfArgumentIsEnclosed(arg, EnclosingType.QUOTES).ifPresent(msg::append);
						CommanderInterface.getValueIfArgumentMatches(arg, "l").map(LoggingWrapper::parseLogLevel).ifPresent(lvl::set);
					}
					
					if (!msg.isEmpty()) {
						log().log(lvl.get(), msg.toString());
					}
				}
				
				return true;
			}
			case console -> {
				KrystalFramework.startConsole();
				return true;
			}
			case cls -> {
				Optional.ofNullable(KrystalFramework.getConsole()).ifPresent(ConsoleView::clear);
				return true;
			}
			case props -> {
				if (arguments.isEmpty()) {
					logConsole("""
					           <h2>application properties management</h2>
					           <p>Specify the arguments of the props command (can repeat within command):</p>
					           <dl>
					           <dt><b><i>l, list</i></b></dt><dd>Lists all properties derived from <code>application.properties</code> and command line args.</dd>
					           <dt><b><i>rm</i></b> property_name</dt><dd>Removes property.</dd>
					           <dt><b><i>-[-]property_name</i></b> property_value</dt><dd>If only name provided, lists property value. If value provided - sets as new.</dd>
					           </dl>
					           """);
				}
				
				// for enquoted values
				val prevProp = new AtomicReference<PropertiesInterface>();
				
				for (var arg : arguments) {
					
					if (CommanderInterface.argumentIsEnclosed(arg, EnclosingType.QUOTES)) {
						val pp = prevProp.get();
						if (pp != null) {
							Optional.ofNullable(CommanderInterface.getEnclosedValue(arg)).ifPresent(v -> {
								logConsole(">>> Set value for property \"%s\": %s".formatted(pp.name(), v));
								pp.set(v);
								prevProp.set(null);
							});
						}
						continue;
					}
					
					if (CommanderInterface.argumentMatches(arg, "l", "list")) {
						logConsole(">>> Stored Properties: " + PropertiesInterface.printAll());
						continue;
					}
					
					if (CommanderInterface.argumentMatches(arg, "rm")) {
						Optional.ofNullable(CommanderInterface.getArgumentValue(arg)).ifPresentOrElse(p -> {
							if (PropertiesInterface.properties.remove(p) != null) {
								logConsole(">>> Property removed: \"%s\".".formatted(p));
							} else {
								logConsole(">>> Property \"%s\" is not set.".formatted(p));
							}
						}, () -> logConsole(">>> Property name missing for remove command."));
						continue;
					}
					
					val prop = PropertiesInterface.of(CommanderInterface.getArgumentName(arg));
					Optional.ofNullable(CommanderInterface.getArgumentValue(arg)).ifPresentOrElse(value -> {
						prop.set(value);
						logConsole(">>> Set value for property \"%s\": %s".formatted(prop.name(), value));
					}, () -> {
						prevProp.set(prop); // if value was provided in quotes - deal in next iter
						prop.value().ifPresentOrElse(v -> logConsole(">>> Current value for property \"%s\": %s".formatted(prop.name(), v)), () -> logConsole(">>> Property \"%s\" is not set.".formatted(prop.name())));
					});
				}
				
				return true;
			}
			case providers -> {
				QueryExecutorInterface.getInstance().ifPresent(q -> logConsole(">>> Loaded DefaultProviders properties:\n%s".formatted(JSON.fromObject(q.getConnectionProperties()).toString(4))));
				return true;
			}
			case help -> {
				logConsole(">>> List of basic commands: %s".formatted(Arrays.stream(BaseCommands.values()).map(Enum::toString).collect(Collectors.joining(KrystalFramework.getDefaultDelimeter()))));
				return true;
			}
			case spring -> {
				Optional.ofNullable(KrystalFramework.getSpringContext()).ifPresent(c -> logConsole(">>> Loaded %s Spring Beans:\n\n%s".formatted(c.getBeanDefinitionCount(), String.join("\n", c.getBeanDefinitionNames()))));
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
					if (arguments.isEmpty()) {
						val host = tomcat.getHost();
						logConsole("""
						           <h2>Tomcat settings:</h2>
						           <pre>%s</pre>
						           <h3>Mappings:</h3>
						           <dl>
						           %s
						           </dl>
						           """.formatted(
								new JSONObject()
										.put("host", host.getName())
										.put("appBase", host.getAppBase())
										.toString(4),
								Arrays.stream(host.findChildren())
								      .map(Context.class::cast)
								      .map(context -> """
								                      <dt><strong>%s</strong></dt>
								                      <dd>%s</dd>
								                      """.formatted(
										      context.getPath(),
										      Arrays.stream(context.findServletMappings())
										            .collect(Collectors.joining(KrystalFramework.getDefaultDelimeter()))
								      )).collect(Collectors.joining(System.lineSeparator()))
						));
						return true;
					}
					
					val name = new AtomicReference<String>();
					val source = new AtomicReference<String>();
					boolean app = false;
					
					for (var arg : arguments) {
						if (CommanderInterface.argumentMatches(arg, "stop")) {
							tomcat.stop();
							logConsole(">>> Tomcat stopped.");
							return true;
						}
						
						if (CommanderInterface.argumentMatches(arg, "start")) {
							tomcat.start();
							tomcat.getConnector();
							logConsole(">>> Tomcat started.");
							return true;
						}
						
						if (CommanderInterface.argumentMatches(arg, "restart")) {
							tomcat.stop();
							tomcat.start();
							tomcat.getConnector();
							logConsole(">>> Tomcat restarted.");
							return true;
						}
						
						if (CommanderInterface.argumentMatches(arg, "a", "app")) {
							app = true;
						}
						
						CommanderInterface.getValueIfArgumentMatches(arg, "n", "name").ifPresent(name::set);
						CommanderInterface.getValueIfArgumentMatches(arg, "s", "source").ifPresent(source::set);
						CommanderInterface.getValueIfArgumentIsEnclosed(arg, EnclosingType.QUOTES).ifPresent(source::set);
						
					}
					
					if (!app) return true;
					if (Strings.isNullOrEmpty(name.get()) || Strings.isNullOrEmpty(source.get())) {
						log().warn(">>> Tomcat app command requires two arguments: -[-n]ame and -[-s]source to be provided.");
						return true;
					}
					
					if (Tools.getResource(source.get()).isEmpty()) {
						log().warn(">>> Provided app source not found.");
						return false;
					}
					
					TomcatFactory.addApp(tomcat, name.get(), source.get());
					
				} catch (LifecycleException e) {
					log().fatal("!!! Tomcat broke with exception:\n{}", e.getMessage());
				}
				
				return true;
			}
			default -> {
				return false;
			}
		}
	}
	
}