package framework.core;

import framework.KrystalFramework;
import framework.logging.LoggingInterface;
import krystal.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

@FunctionalInterface
public interface PropertyInterface extends LoggingInterface {
	
	Map<String, Object> properties = new HashMap<>();
	
	static void loadCmdLnArgs(String... args) {
		Stream.of(String.join(" ", args).split("--")).skip(1).forEach(s -> {
			String[] parts = s.trim().split("[\\s=]", 2);
			Object value = null;
			try {
				value = properlyCast(parts[1]);
			} catch (IndexOutOfBoundsException ignored) {
			}
			properties.put(parts[0], value);
		});
	}
	
	static void loadAppProperties(String propertiesPath) {
		try (InputStream source = Tools.getResource(propertiesPath).openStream()) {
			Properties props = new Properties();
			props.load(source);
			for (var prop : props.stringPropertyNames()) {
				Object value = null;
				try {
					value = properlyCast(props.getProperty(prop));
				} catch (NullPointerException ignored) {
				}
				properties.put(prop, value);
			}
		} catch (IOException ignored) {
		}
	}
	
	static void load(String propertiesPath, String... args) {
		loadCmdLnArgs(args);
		loadAppProperties(propertiesPath);
	}
	
	private static Object properlyCast(String arg) {
		try {
			return Integer.parseInt(arg);
		} catch (Exception ignored) {
		}
		
		try {
			return Double.parseDouble(arg);
		} catch (Exception ignored) {
		}
		
		return switch (arg) {
			case "yes", "no", "true", "false" -> Boolean.parseBoolean(arg);
			default -> arg;
		};
	}
	
	static String printAll() {
		return Tools.concat(KrystalFramework.getDefaultDelimeter(), properties.entrySet().stream().map(e -> String.format("[%s = %s]", e.getKey(), e.getValue())));
	}
	
	static boolean areAny() {
		return !properties.values().isEmpty();
	}
	
	String name();
	
	/**
	 * @return Optional command's value.
	 */
	default Optional<Object> value() {
		return Optional.ofNullable(properties.get(name()));
	}
	
	/**
	 * @return True if the command is present in the line.
	 */
	default boolean isPresent() {
		return properties.containsKey(name());
	}
	
}