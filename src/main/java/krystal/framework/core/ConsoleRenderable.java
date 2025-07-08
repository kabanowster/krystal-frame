package krystal.framework.core;

import krystal.ConsoleView;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingWrapper;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.jsoup.nodes.Element;

import java.awt.EventQueue;
import java.util.Optional;

@Log4j2
public abstract class ConsoleRenderable {
	
	private ConsoleView console = KrystalFramework.getConsole();
	
	protected abstract String getId();
	
	/**
	 * Set the position of the rendered progress bar, regarding the content of the ConsoleView.
	 */
	private Position position = Position.after;
	/**
	 * Applicable when {@link Position#logger} is selected for {@link #position};
	 */
	private Level loggerLevel;
	
	public ConsoleRenderable console(ConsoleView console) {
		this.console = console;
		return this;
	}
	
	public ConsoleRenderable position(Position position) {
		this.position = position;
		return this;
	}
	
	/**
	 * @see #level(Level)
	 */
	public ConsoleRenderable level(String level) {
		return level(LoggingWrapper.parseLogLevel(level));
	}
	
	/**
	 * Also automatically sets position to {@link Position#logger}
	 */
	public ConsoleRenderable level(Level level) {
		this.position = Position.logger;
		this.loggerLevel = level;
		return this;
	}
	
	public ConsoleRenderable render() {
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
		EventQueue.invokeLater(() -> console.revalidate());
		return this;
	}
	
	public void dispose() {
		getDocElement().ifPresent(Element::remove);
		EventQueue.invokeLater(() -> console.revalidate());
	}
	
	private Optional<Element> getDocElement() {
		if (console == null) return Optional.empty();
		return Optional.ofNullable(console.getContentDocument().getElementById(getId()));
	}
	
	public enum Position {
		after, before, append, logger
	}
	
}