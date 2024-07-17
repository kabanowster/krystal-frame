package krystal.framework.commander;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * To read commands from outside of app, use {@link #readCommandsFromTextFile(File)}, which can be scheduled with your flow manager. Commands can be defined with {@link CommandInterface} as enums and parsed through
 * {@link #executeCommand(CommandInterface, List)}.
 * <p>
 * <h3>Commands parsing patterns</h3>
 * <ul>
 *     <li>The first word separated by any number of {@code space} chars is a command, parsable as {@link CommandInterface}.</li>
 *     <li>Strings following the command are recognised as argument-value pairs. The spacing <strong>does not</strong> influence the pattern recognition.</li>
 *     <li>Unmatched pairs, null, quoted or bracketed, and the {@code --} arg - all are treated as <i><b>empty</b></i> arguments (they hold no assigned value information, will return {@code null}).</li>
 *     <li>Value is assigned using any number of following characters: {@code space}, {@code =} or {@code :}.</li>
 *     <li>Enquoted args do not permit quote characters within (they close the pattern).</li>
 *     <li>Bracketed args allow of usage any character within, including other brackets (the last bracket closes the pattern).</li>
 *     <li>The {@code --} arg escapes any pattern parsing for any string put after.</li>
 *     <li>Single and double quotes, different brackets - are not distinguished.</li>
 *     <li>{@code -} and {@code --} as prefixes are treated with no difference. Args get stripped from these when parsing. Using dashes for arguments make the parse error-proof and values assignments clear.</li>
 * </ul>
 *
 * @see #parseCommand(String)
 * @see #getValueIfArgumentMatches(String, String...)
 * @see #getValueIfArgumentIsEnclosed(String, EnclosingType)
 * @see #argumentMatches(String, String...)
 * @see #argumentIsEnclosed(String, EnclosingType)
 * @see #getArgumentValue(String)
 * @see #getEnclosedValue(String)
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
	 *
	 * @return Boolean - true if the command was correctly parsed (irrespectively to the outcome), false if not found or errored. Susceptible to custom interpretation.
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
		val line = command.split(" +", 2);
		return executeCommand(
				() -> line[0].strip().toLowerCase(),
				line.length > 1 ? getArguments(line[1]) : List.of()
		);
	}
	
	/**
	 * Use pattern recognition regex to split the provided line of arguments into argument-value pairs or single arguments/values.
	 */
	static List<String> getArguments(String argumentsLine) {
		return Stream.of(argumentsLine.splitWithDelimiters("([\"'].+[\"'])|([[({<].+[\\\\])}>])|(-{1,2}\\w+(-\\w+)*[ =:]+(\\w+[,;|]*)*)|(-- .*)", 0))
		             .map(String::strip)
		             .map(s -> s.replaceAll("^-{1,2}(?=.*?)", ""))
		             .filter(s -> !Strings.isNullOrEmpty(s))
		             .toList();
	}
	
	/**
	 * Return the value if argument matches any of the provided variants.
	 */
	static Optional<String> getValueIfArgumentMatches(String argument, String... variants) {
		if (argumentMatches(argument, variants)) {
			return Optional.ofNullable(getArgumentValue(argument));
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Return the value if argument is enclosed in {@link EnclosingType}.
	 */
	static Optional<String> getValueIfArgumentIsEnclosed(String argument, EnclosingType within) {
		if (argumentIsEnclosed(argument, within)) {
			return Optional.ofNullable(getEnclosedValue(argument));
		} else {
			return Optional.empty();
		}
	}
	
	/**
	 * Checks if argument fits the valid pattern, and if it's name is one of the variants.
	 */
	static boolean argumentMatches(@Nullable String argument, String... variants) {
		if (argument == null) return false;
		val regex = "^(%s)([ =:]+.*?)?$".formatted(String.join("|", Arrays.stream(variants).map(Tools::escapeRegexSpecials).toList()));
		return argument.matches(regex);
	}
	
	static boolean argumentValid(@Nullable String argument) {
		if (argument == null) return false;
		return argument.matches("\\w+(-\\w+)*[ =:]+(\\w+,*)*");
	}
	
	/**
	 * Checks if the argument is enquoted or bracketed value.
	 */
	static boolean argumentIsEnclosed(@Nullable String argument, EnclosingType within) {
		if (argument == null) return false;
		val regex = switch (within) {
			case QUOTES -> "^[\"'].+?[\"']$";
			case BRACKETS -> "^[[(<{].+?[\\\\])>}]$";
		};
		return argument.matches(regex);
	}
	
	/**
	 * Gets the value of the argument, issued with either {@code " "}, {@code "="} or {@code ":"}, or returns {@code null}.
	 */
	static @Nullable String getArgumentValue(@NonNull String argument) {
		try {
			return argument.split("[ =:]+", 2)[1].strip().transform(s -> s.isEmpty() ? null : s);
		} catch (IndexOutOfBoundsException _) {
			return null;
		}
	}
	
	/**
	 * Return argument's name.
	 */
	static String getArgumentName(@NonNull String argument) {
		return argument.split("[ =:]+", 2)[0];
	}
	
	/**
	 * Gets the value of the enclosed argument. Effectively substring between first and last character.
	 */
	static @Nullable String getEnclosedValue(@NonNull String argument) {
		try {
			return argument.substring(1, argument.length() - 1);
		} catch (IndexOutOfBoundsException _) {
			return null;
		}
	}
	
	enum EnclosingType {
		QUOTES, BRACKETS
	}
	
}