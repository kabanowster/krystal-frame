package krystal.framework.core;

import krystal.ConsoleView;
import krystal.StringRenderer.ProgressRenderer;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingWrapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.io.IOException;
import java.util.Optional;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
public class ConsoleProgress extends ProgressRenderer {
	
	private ConsoleView console = KrystalFramework.getConsole();
	/**
	 * Set the position of the rendered progress bar, regarding the content of the ConsoleView.
	 */
	private Position position = Position.after;
	/**
	 * Applicable when {@link Position#logger} is selected for {@link #position};
	 */
	private Level loggerLevel;
	
	public ConsoleProgress(long targetValue) {
		setTarget(targetValue);
	}
	
	public ConsoleProgress console(ConsoleView console) {
		this.console = console;
		return this;
	}
	
	public ConsoleProgress position(Position position) {
		this.position = position;
		return this;
	}
	
	/**
	 * @see #level(Level)
	 */
	public ConsoleProgress level(String level) {
		return level(LoggingWrapper.parseLogLevel(level));
	}
	
	/**
	 * Also automatically sets position to {@link Position#logger}
	 */
	public ConsoleProgress level(Level level) {
		this.position = Position.logger;
		this.loggerLevel = level;
		return this;
	}
	
	@Override
	public ConsoleProgress render() {
		getDocElement().ifPresentOrElse(
				element -> {
					try {
						console.getDoc().setOuterHTML(element, toString());
					} catch (BadLocationException | IOException | NullPointerException e) {
						log.error("ProgressRenderer: Unable to update body. {}", e.getMessage());
					}
				},
				() -> {
					try {
						switch (position) {
							case after -> console.getDoc().insertAfterEnd(console.getContent(), toString());
							case before -> console.getDoc().insertBeforeStart(console.getContent(), toString());
							case append -> console.getDoc().insertBeforeEnd(console.getContent(), toString());
							case logger -> log.log(Optional.ofNullable(loggerLevel).orElse(LoggingWrapper.ROOT_LOGGER.getLevel()), toString());
						}
					} catch (BadLocationException | IOException | NullPointerException e) {
						log.error("ProgressRenderer: Unable to insert body. {}", e.getMessage());
					}
				}
		);
		return this;
	}
	
	@Override
	public void dispose() {
		getDocElement().ifPresent(
				e -> {
					try {
						console.getDoc().remove(e.getStartOffset(), e.getEndOffset() - e.getStartOffset());
					} catch (NullPointerException | BadLocationException _) {
					}
				}
		);
	}
	
	private Optional<Element> getDocElement() {
		if (console == null) return Optional.empty();
		return Optional.ofNullable(console.getDoc().getElement(getId()));
	}
	
	public enum Position {
		after, before, append, logger
	}
	
}