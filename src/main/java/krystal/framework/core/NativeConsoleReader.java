package krystal.framework.core;

import krystal.VirtualPromise;
import krystal.framework.KrystalFramework;
import krystal.framework.commander.CommanderInterface;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Scanner;

/**
 * The process manager to read commands from native console input ({@link System#in}).
 * For more sophisticated console render, use {@link KrystalFramework#startConsole(String)}.
 *
 * @see CommanderInterface
 * @see krystal.ConsoleView
 */
@Log4j2
@Service
public class NativeConsoleReader {
	
	private @Nullable VirtualPromise<Void> process;
	private final Scanner input;
	private final CommanderInterface commander;
	
	@Autowired
	public NativeConsoleReader(CommanderInterface commander) {
		input = new Scanner(System.in);
		this.commander = commander;
		start();
	}
	
	public void start() {
		if (process != null) {
			log.info("=== Reading native console is already started.");
			return;
		}
		
		process = VirtualPromise.run(() -> {
			while (input.hasNextLine()) {
				commander.parseCommand(input.nextLine());
			}
		});
		process.thenClose();
	}
	
	public void stop() {
		if (process == null) {
			log.info("=== There is no active native console reading process.");
			return;
		}
		
		process.kill();
		process = null;
	}
	
}