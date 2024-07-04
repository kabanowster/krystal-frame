package krystal.framework.core;

import krystal.ConsoleView;
import krystal.StringRenderer.ProgressRenderer;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingWrapper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.jsoup.nodes.Element;

import java.awt.EventQueue;
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
	
	public ConsoleProgress(String unit) {
		setUnit(unit);
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
		EventQueue.invokeLater(() -> {
			getDocElement().ifPresentOrElse(
					element -> element.html(toString()),
					() -> {
						switch (position) {
							case after -> console.getContent().after(toString());
							case before -> console.getContent().before(toString());
							case append -> console.getContent().append(toString());
							case logger -> log.log(Optional.ofNullable(loggerLevel).orElse(LoggingWrapper.ROOT_LOGGER.getLevel()), toString());
						}
					}
			);
			console.revalidate();
		});
		return this;
	}
	
	@Override
	public void dispose() {
		EventQueue.invokeLater(() -> {
			getDocElement().ifPresent(Element::remove);
			console.revalidate();
		});
	}
	
	private Optional<Element> getDocElement() {
		if (console == null) return Optional.empty();
		return Optional.ofNullable(console.getContentDocument().getElementById(getId()));
	}
	
	public enum Position {
		after, before, append, logger
	}
	
}