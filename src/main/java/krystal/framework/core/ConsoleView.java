package krystal.framework.core;

import krystal.framework.KrystalFramework;
import krystal.framework.commander.CommanderInterface;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.val;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;

public class ConsoleView implements LoggingInterface {
	
	private StringBuilder messages;
	private final Font font;
	private final JFrame frame;
	private final JLabel output;
	private final JScrollPane scroll;
	private final JCheckBox optionAutoScroll;
	private final JTextField commandPrompt;
	private final int MAX_BUFFER_SIZE = (int) Math.pow(2, 25);
	private static volatile boolean renderInbound;
	
	/**
	 * Creates a simple Swing window to output log events.
	 */
	public ConsoleView() {
		// messages holder
		messages = new StringBuilder();
		
		/*
		 * Render Swing Console
		 */
		font = new Font("monospaced", Font.PLAIN, 14);
		
		output = new JLabel();
		output.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		output.setAlignmentY(JComponent.TOP_ALIGNMENT);
		output.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		output.setVerticalAlignment(JLabel.TOP);
		output.setVerticalTextPosition(JLabel.TOP);
		output.setHorizontalAlignment(JLabel.LEFT);
		output.setHorizontalTextPosition(JLabel.LEFT);
		output.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
		output.setBackground(Color.getHSBColor(0.4583f, 0.15f, 0.15f));
		output.setForeground(Color.lightGray);
		output.setOpaque(true);
		output.setFont(font);
		
		commandPrompt = new JTextField();
		commandPrompt.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		commandPrompt.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
		commandPrompt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
		commandPrompt.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		commandPrompt.setFont(font);
		commandPrompt.setBackground(Color.getHSBColor(0.5278f, 0.15f, 0.25f));
		commandPrompt.setForeground(Color.lightGray);
		commandPrompt.setCaretColor(Color.white);
		commandPrompt.addActionListener((e) -> {
			val executor = CommanderInterface.getInstance();
			if (executor != null)
				executor.parseCommand(commandPrompt.getText());
			commandPrompt.setText(null);
			commandPrompt.requestFocus();
		});
		
		val view = new JPanel();
		view.setLayout(new BoxLayout(view, BoxLayout.Y_AXIS));
		view.add(output);
		view.add(commandPrompt);
		
		scroll = new JScrollPane(
				view,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, //
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED //
		);
		scroll.setBorder(BorderFactory.createLineBorder(Color.getHSBColor(0.5278f, 0.15f, 0.20f), 8));
		scroll.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		scroll.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		
		val options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
		options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		options.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		options.setAlignmentY(JComponent.TOP_ALIGNMENT);
		options.setBackground(Color.getHSBColor(0.5278f, 0.15f, 0.20f));
		options.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		optionAutoScroll = new JCheckBox("Autoscroll to bottom");
		optionAutoScroll.setSelected(true);
		optionAutoScroll.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		optionAutoScroll.setForeground(Color.white);
		optionAutoScroll.setOpaque(false);
		options.add(optionAutoScroll);
		
		options.add(Box.createHorizontalGlue());
		
		val btnScroll = new JButton("Bottom");
		btnScroll.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
		btnScroll.addActionListener((e) -> scrollToBottom());
		btnScroll.setMaximumSize(new Dimension(100, 20));
		options.add(btnScroll);
		
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(1200, 600);
		frame.setTitle("Console View");
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		
		frame.add(options);
		frame.add(scroll);
		frame.revalidate();
		frame.setVisible(true);
		commandPrompt.requestFocus();
		
		/*
		 * Set-up appender
		 */
		
		val layout = PatternLayout.newBuilder()
		                          .setPattern(KrystalFramework.getLoggingPattern())
		                          .build();
		
		val appender = new AbstractAppender("ConsoleView", null, layout, true, null) {
			
			@Override
			public void append(LogEvent event) {
				layout.serialize(event, messages);
				// messages.append(layout.toSerializable(event));
				limitBuffer();
				if (!renderInbound)
					EventQueue.invokeLater(() -> renderMessages());
			}
			
		};
		
		LoggingWrapper.ROOT_LOGGER.addAppender(appender);
		appender.start();
		
		log().fatal("=== Custom Console Viewer created and logger wired.");
	}
	
	private synchronized void renderMessages() {
		output.setText("<html><body><pre>%s</pre></body><html/>".formatted(messages.toString()));
		frame.revalidate();
		renderInbound = false;
		if (optionAutoScroll.isSelected()) scrollToBottom();
	}
	
	private void limitBuffer() {
		val l = messages.length() - MAX_BUFFER_SIZE;
		if (l > 0) messages.delete(0, l);
	}
	
	/**
	 * Scroll down to last message and focus on input field.
	 */
	public void scrollToBottom() {
		val bar = scroll.getVerticalScrollBar();
		bar.setValue(bar.getMaximum());
		commandPrompt.requestFocus();
	}
	
	/**
	 * Clear log messages from the view.
	 */
	public void clear() {
		messages = new StringBuilder();
		logConsole(">>> Clear");
		renderMessages();
	}
	
	/**
	 * @see JFrame#dispose()
	 */
	public void dispose() {
		frame.dispose();
	}
	
}