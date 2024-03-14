package krystal.framework.core;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import tornadofx.control.DateTimePicker;

/**
 * Base container of JavaFX application and utilities.
 */
@Log4j2
@Getter
@Setter
public class jfxApp extends Application implements LoggingInterface {
	
	private static @Setter Runnable runnableWithinStart;
	private Stage primaryStage;
	
	public static void scrollToNode(ScrollPane scrollPane, Node node) {
		Bounds bounds = node.getBoundsInParent();
		
		// Calculate the vertical and horizontal positions
		double vValue = bounds.getMinY() / (scrollPane.getContent().getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight());
		double hValue = bounds.getMinX() / (scrollPane.getContent().getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth());
		
		// Set the scroll positions
		scrollPane.setVvalue(vValue);
		scrollPane.setHvalue(hValue);
	}
	
	/**
	 * Resets the input and output nodes (empties them). Some only if they don't have id. Pane nodes descendants included.
	 */
	public static void resetNodes(Pane pane) {
		
		pane.getChildren().forEach(n -> {
			val clazz = n.getClass();
			
			if (Pane.class.isAssignableFrom(clazz))
				resetNodes((Pane) n);
			else if (ComboBox.class.isAssignableFrom(clazz))
				((ComboBox<?>) n).getSelectionModel().clearSelection();
			else if (ChoiceBox.class.isAssignableFrom(clazz))
				((ChoiceBox<?>) n).getSelectionModel().clearSelection();
			else if (TextField.class.isAssignableFrom(clazz))
				((TextField) n).setText(null);
			else if (TextArea.class.isAssignableFrom(clazz))
				((TextArea) n).setText(null);
			else if (ListView.class.isAssignableFrom(clazz))
				((ListView<?>) n).getItems().clear();
			else if (Label.class.isAssignableFrom(clazz)) {
				Label l = (Label) n;
				if (l.getId() == null)
					l.setText(null);
			} else if (CheckBox.class.isAssignableFrom(clazz))
				((CheckBox) n).setSelected(false);
			else if (RadioButton.class.isAssignableFrom(clazz))
				((RadioButton) n).setSelected(false);
			else if (DateTimePicker.class.isAssignableFrom(clazz))
				((DateTimePicker) n).setDateTimeValue(null);
			else if (DatePicker.class.isAssignableFrom(clazz))
				((DatePicker) n).setValue(null);
			else if (ScrollPane.class.isAssignableFrom(clazz)) ;// skip
			else if (Button.class.isAssignableFrom(clazz)) ;// skip
			else if (ProgressIndicator.class.isAssignableFrom(clazz)) ;// skip
			else
				log.warn(String.format("!!! resetNodes(%s): Class not supported: %s", pane.getClass().getSimpleName(), clazz.getSimpleName()));
		});
	}
	
	@Override
	public void start(Stage primaryStage) {
		
		this.primaryStage = primaryStage;
		KrystalFramework.setJfxApplication(this);
		if (runnableWithinStart != null) runnableWithinStart.run();
	}
	
}