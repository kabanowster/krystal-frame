package krystal.framework.commander;

import com.google.common.io.Files;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * To read external commands, use {@link #readCommandsFromTextFile(File)}, which can be scheduled with your flow manager. Commands can be defined with {@link CommandInterface} as enums and parsed through {@link #executeCommand(CommandInterface, List)} for
 * clarity or use {@link #parseCommand(String)}. First word in line is a command and arguments delimiter is "--". The split-part before first "--" is skipped. Further parse argument with {@link #getValueIfArgumentIs(String, String...)}.
 */
public interface CommanderInterface extends LoggingInterface {
	
	static Optional<CommanderInterface> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(CommanderInterface.class));
		} catch (NullPointerException e) {
			return Optional.empty();
		} catch (NoSuchBeanDefinitionException e) {
			LoggingInterface.logger().fatal(e.getMessage());
			return Optional.empty();
		}
	}
	
	/**
	 * Implement the execution of given command and arguments. Using switch with enum implementing {@link CommandInterface} is advised.
	 */
	boolean executeCommand(CommandInterface command, List<String> arguments);
	
	/**
	 * Read file lines passed and perform {@link #executeCommand(CommandInterface, List)} on each.
	 */
	default void readCommandsFromTextFile(File commandsFile) {
		try {
			Files.readLines(commandsFile, StandardCharsets.UTF_8)
			     .forEach(line -> {
				     log().fatal("*** EXTERNAL COMMAND PARSING: {}", line);
				     parseCommand(line);
			     });
		} catch (IOException ex) {
			// log().fatal("!!! External commands file not responding.");
		}
	}
	
	/**
	 * First trimmed word is a command, case-insensitive, followed by arguments with "--" or "-".
	 */
	default boolean parseCommand(String command) {
		val line = command.split(" ", 2);
		return executeCommand(
				() -> line[0].strip().toLowerCase(),
				// line.length > 1 ? (line[1].transform((a) -> a.matches(".*?--.+?") ? Stream.of(a.split("--")).map(String::strip).filter(s -> !s.isEmpty()).toList() : List.of(a))) : List.of()
				line.length > 1 ? Stream.of(line[1].split("--|\\s(?=-[a-zA-Z])")).map(String::strip).filter(s -> !s.isEmpty()).toList() : List.of()
		);
	}
	
	/**
	 * If provided argument (String) matches pattern and any of the name variants, returns its value.
	 */
	default Optional<String> getValueIfArgumentIs(String argument, String... variants) {
		if (argumentMatches(argument, variants)) {
			return Optional.ofNullable(getArgumentValue(argument));
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Checks if argument fits the valid pattern, and if it's name is one of the variants.
	 */
	default boolean argumentMatches(@Nullable String argument, String... variants) {
		if (argument == null) return false;
		val regex = "^(%s)([\\s=]\\w[\\w\\W]*?)?$".formatted(String.join("|", variants));
		return argument.matches(regex);
	}
	
	/**
	 * Gets the value of the argument, issued with either " " or "=".
	 */
	default String getArgumentValue(@NonNull String argument) {
		return argument.split("[\\s=]", 2)[1].strip().transform(s -> s.isEmpty() ? null : s);
	}
	
}