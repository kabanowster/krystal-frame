package framework.core;

import framework.KrystalFramework;
import framework.logging.LoggingInterface;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
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
// TODO use preloader instead?
public class JavaFXApplication extends Application implements LoggingInterface {
	
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
	 * Resets the input and output components (empties them). Some only if they have names. Children included. for which all inner containers and components will get reset.
	 */
	// TODO change instanceof to assignablefrom
	public static void resetNodes(Pane pane) {
		
		pane.getChildren().forEach(n -> {
			val clazz = n.getClass();
			
			if (Pane.class.isAssignableFrom(clazz))
				resetNodes((Pane) n);
			else if (n instanceof ComboBox<?> node)
				node.getSelectionModel().clearSelection();
			else if (n instanceof ChoiceBox<?> node)
				node.getSelectionModel().clearSelection();
			else if (n instanceof TextField node)
				node.setText(null);
			else if (n instanceof TextArea node)
				node.setText(null);
			else if (n instanceof ListView<?> node)
				node.getItems().clear();
			else if (n instanceof Label node) {
				if (node.getId() == null)
					node.setText(null);
			} else if (n instanceof CheckBox node)
				node.setSelected(false);
			else if (n instanceof RadioButton node)
				node.setSelected(false);
			else if (n instanceof DateTimePicker node)
				node.setDateTimeValue(null);
			else if (n instanceof DatePicker node)
				node.setValue(null);
			else if (n instanceof ScrollPane node) ;// skip
			else if (n instanceof Button node) ;// skip
			else if (n instanceof ProgressIndicator node) ;// skip
			else
				log.warn(String.format("!!! resetNodes(%s): Class not supported: %s", pane.getClass().getSimpleName(), clazz.getSimpleName()));
		});
	}
	
	@Override
	public void start(Stage primaryStage) {
		
		/*
		 * Base Initialization
		 */
		
		this.primaryStage = primaryStage;
		KrystalFramework.setJavaFXApplication(this);
	}
	
}