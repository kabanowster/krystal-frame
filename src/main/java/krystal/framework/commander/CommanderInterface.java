package krystal.framework.commander;

import com.google.common.io.Files;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * To read external commands, provide a source text file, where each line will be parsed - first word as command and following as arguments, separated with space character. Attach {@link CommandInterface} to enum and parse it through
 * {@link #executeCommand(CommandInterface, List)} for clarity.
 */
public interface CommanderInterface extends LoggingInterface {
	
	static CommanderInterface getInstance() {
		return KrystalFramework.getSpringContext().getBean(CommanderInterface.class);
	}
	
	/**
	 * Implement the execution of given command and arguments. Using switch with enum implementing {@link CommandInterface} is advised.
	 */
	void executeCommand(CommandInterface command, List<String> arguments);
	
	/**
	 * Put the file to read here.
	 */
	File getCommanderFile();
	
	/**
	 * Read file lines passed with {@link #getCommanderFile()} and perform {@link #executeCommand(CommandInterface, List)} on each.
	 */
	default void readCommandsFromTextFile() {
		try {
			Files.readLines(getCommanderFile(), StandardCharsets.UTF_8)
			     .forEach(lineString -> {
				     log().fatal("*** EXTERNAL COMMAND PARSING: " + lineString);
				     String[] arguments = lineString.split(" ");
				     executeCommand(
						     () -> Stream.of(arguments)
						                 .findFirst()
						                 .orElseThrow()
						                 .trim()
						                 .toLowerCase(),
						     Stream.of(arguments).skip(1).toList()
				     );
			     });
		} catch (IOException ex) {
			// log().fatal("!!! External commands file not responding.");
		}
	}
	
}