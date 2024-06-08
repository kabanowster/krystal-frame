package krystal.framework.core;

import com.google.common.base.Strings;
import krystal.framework.KrystalFramework;
import krystal.framework.commander.CommanderInterface;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ConsoleView implements LoggingInterface {
	
	private final JFrame frame;
	private final JTextPane output;
	private final StyledDocument doc;
	private final JScrollPane scroll;
	private final JCheckBox optionAutoScroll;
	private final JTextField commandPrompt;
	private final Font defaultFont = new Font("monospaced", Font.PLAIN, 14);
	private final List<String> commandStack = new ArrayList<>(List.of(""));
	private int commandSelected;
	
	/**
	 * Creates a simple Swing window to output log events.
	 */
	public ConsoleView() {
		/*
		 * Render Swing Console
		 */
		
		val outerColor = Color.getHSBColor(0.5278f, 0.15f, 0.20f);
		val innerColor = Color.getHSBColor(0.5278f, 0.10f, 0.16f);
		val middleColor = Color.getHSBColor(0.5278f, 0.15f, 0.25f);
		
		output = new JTextPane();
		output.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
		output.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		output.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		output.setAlignmentY(JComponent.TOP_ALIGNMENT);
		output.setBackground(innerColor);
		output.setContentType("text/html");
		((DefaultCaret) output.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		
		createStyles(output);
		
		doc = output.getStyledDocument();
		
		commandPrompt = new JTextField();
		commandPrompt.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		commandPrompt.setAlignmentY(JComponent.BOTTOM_ALIGNMENT);
		commandPrompt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
		commandPrompt.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		commandPrompt.setFont(defaultFont);
		commandPrompt.setBackground(middleColor);
		commandPrompt.setForeground(Color.lightGray);
		commandPrompt.setCaretColor(Color.white);
		commandPrompt.addActionListener((e) -> {
			val command = commandPrompt.getText();
			if (Strings.isNullOrEmpty(command))
				return;
			commandStack.set(0, command);
			CommanderInterface.getInstance().ifPresent(ci -> ci.parseCommand(command));
			commandStack.addFirst("");
			commandPrompt.setText(null);
			commandPrompt.requestFocus();
		});
		commandPrompt.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				commandSelected = 0;
				if (!Character.isISOControl(e.getKeyChar()))
					commandStack.set(0, commandPrompt.getText() + e.getKeyChar());
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				val key = e.getKeyCode();
				val historyMax = commandStack.size() - 1;
				
				if (key == KeyEvent.VK_UP) {
					commandPrompt.setText(commandStack.get(commandSelected == historyMax ? historyMax : ++commandSelected));
				}
				
				if (key == KeyEvent.VK_DOWN) {
					commandPrompt.setText(commandStack.get(commandSelected > 0 ? --commandSelected : 0));
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			
			}
		});
		
		scroll = new JScrollPane(
				output,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, //
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED //
		);
		scroll.setBorder(BorderFactory.createLineBorder(outerColor, 8));
		scroll.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		scroll.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		scroll.setBackground(innerColor);
		
		Supplier<BasicScrollBarUI> scrollBar = () -> new BasicScrollBarUI() {
			@Override
			protected JButton createDecreaseButton(int orientation) {
				return new BasicArrowButton(orientation, middleColor, middleColor, innerColor.darker(), middleColor);
			}
			
			@Override
			protected JButton createIncreaseButton(int orientation) {
				return new BasicArrowButton(orientation, middleColor, middleColor, innerColor.darker(), middleColor);
			}
			
			@Override
			protected void configureScrollBarColors() {
				this.thumbColor = middleColor;
				this.trackColor = innerColor;
				this.trackHighlightColor = innerColor;
			}
		};
		
		scroll.getVerticalScrollBar().setUI(scrollBar.get());
		scroll.getHorizontalScrollBar().setUI(scrollBar.get());
		
		val options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
		options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		options.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		options.setAlignmentY(JComponent.TOP_ALIGNMENT);
		options.setBackground(outerColor);
		options.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		optionAutoScroll = new JCheckBox("Autoscroll to bottom");
		optionAutoScroll.setSelected(true);
		optionAutoScroll.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		optionAutoScroll.setForeground(Color.white);
		optionAutoScroll.setOpaque(false);
		optionAutoScroll.addActionListener(e -> {
			commandPrompt.requestFocus();
		});
		options.add(optionAutoScroll);
		
		options.add(Box.createHorizontalGlue());
		
		val btnScroll = new JButton("Bottom");
		btnScroll.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
		btnScroll.addActionListener((e) -> scrollToBottom());
		btnScroll.setMaximumSize(new Dimension(100, 20));
		options.add(btnScroll);
		
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(1350, 700);
		frame.setTitle("Krystal Frame: Console View");
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		
		val canvas = new JPanel();
		canvas.setLayout(new BoxLayout(canvas, BoxLayout.Y_AXIS));
		canvas.setBorder(BorderFactory.createEmptyBorder(5, 5, 25, 5));
		canvas.setBackground(outerColor);
		canvas.add(options);
		canvas.add(scroll);
		canvas.add(commandPrompt);
		
		frame.add(canvas);
		
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
				EventQueue.invokeLater(() -> {
					try {
						doc.insertString(doc.getLength(), layout.toSerializable(event), Optional.ofNullable(output.getStyle(event.getLevel().name().toLowerCase())).orElse(output.getStyle("default")));
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}
					revalidate();
				});
			}
			
		};
		
		LoggingWrapper.ROOT_LOGGER.addAppender(appender);
		appender.start();
		
		log().fatal("=== Custom Console Viewer created and logger wired.");
	}
	
	private void createStyles(JTextPane textPane) {
		val defaultStyle = textPane.addStyle("default", null);
		StyleConstants.setForeground(defaultStyle, Color.lightGray);
		StyleConstants.setFontFamily(defaultStyle, defaultFont.getFamily());
		StyleConstants.setFontSize(defaultStyle, defaultFont.getSize());
		
		var style = textPane.addStyle("info", defaultStyle);
		StyleConstants.setForeground(style, new Color(0, 153, 255));
		
		style = textPane.addStyle("debug", defaultStyle);
		
		style = textPane.addStyle("fatal", defaultStyle);
		StyleConstants.setForeground(style, new Color(255, 51, 0));
		
		style = textPane.addStyle("test", defaultStyle);
		StyleConstants.setForeground(style, new Color(255, 204, 0));
		
		style = textPane.addStyle("console", defaultStyle);
		StyleConstants.setForeground(style, new Color(51, 204, 51));
		
		style = textPane.addStyle("trace", defaultStyle);
		StyleConstants.setForeground(style, Color.gray.darker());
		
		style = textPane.addStyle("warn", defaultStyle);
		StyleConstants.setForeground(style, new Color(255, 153, 51));
		
		style = textPane.addStyle("error", defaultStyle);
		StyleConstants.setForeground(style, new Color(255, 80, 80));
	}
	
	/**
	 * Scroll down to last message and focus on input field.
	 */
	public void scrollToBottom() {
		val bar = scroll.getVerticalScrollBar();
		bar.setValue(bar.getMaximum());
		commandPrompt.requestFocus();
	}
	
	public void revalidate() {
		frame.revalidate();
		if (optionAutoScroll.isSelected()) scrollToBottom();
	}
	
	/**
	 * Clear log messages from the view.
	 */
	@SneakyThrows
	public void clear() {
		doc.remove(0, doc.getLength());
		logConsole(">>> Clear");
		revalidate();
	}
	
	/**
	 * @see JFrame#dispose()
	 */
	public void dispose() {
		frame.dispose();
	}
	
}