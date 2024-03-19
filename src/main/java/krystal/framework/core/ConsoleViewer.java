package krystal.framework.core;

import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.val;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

public class ConsoleViewer implements LoggingInterface {
	
	private final StringBuffer messages;
	private final JLabel output;
	private final JScrollPane scroll;
	private final int MAX_BUFFER_SIZE = (int) Math.pow(2, 25);
	
	/**
	 * Creates a simple Swing window to output log events.
	 */
	public ConsoleViewer() {
		// messages holder
		messages = new StringBuffer();
		
		/*
		 * Render Swing Console
		 */
		output = new JLabel();
		output.setVerticalAlignment(JLabel.TOP);
		output.setVerticalTextPosition(JLabel.TOP);
		output.setHorizontalAlignment(JLabel.LEFT);
		output.setHorizontalTextPosition(JLabel.LEFT);
		output.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		output.setBackground(Color.black);
		output.setForeground(Color.lightGray);
		output.setOpaque(true);
		output.setFont(new Font("monospaced", Font.PLAIN, 14));
		
		scroll = new JScrollPane(
				output,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, //
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED //
		);
		
		val frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
		frame.add(scroll);
		frame.validate();
		frame.setSize(1200, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		/*
		 * Set-up appender
		 */
		
		val layout = PatternLayout.newBuilder()
		                          .setPattern(KrystalFramework.getLoggingPattern())
		                          .build();
		
		val appender = new AbstractAppender("ConsoleViewer", null, layout, true, null) {
			
			@Override
			public void append(LogEvent event) {
				messages.append(layout.toSerializable(event));
				limitBuffer();
				EventQueue.invokeLater(() -> renderMessages());
			}
			
		};
		
		LoggingWrapper.ROOT_LOGGER.addAppender(appender);
		appender.start();
		
		log().fatal("=== Custom Console Viewer created and logger wired.");
	}
	
	private synchronized void renderMessages() {
		output.setText("<html><pre>%s".formatted(messages.toString()));
	}
	
	private void limitBuffer() {
		val l = messages.length() - MAX_BUFFER_SIZE;
		if (l > 0) messages.delete(0, l);
	}
	
}