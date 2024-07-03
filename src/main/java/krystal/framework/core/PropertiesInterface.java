package krystal.framework.core;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Interface to manage properties and cmd-line arguments.
 *
 * @implNote Access static {@link #properties} map or implement this with {@link Enum} for convenience.
 * @see PropertiesAndArguments
 */
@FunctionalInterface
public interface PropertiesInterface extends LoggingInterface {
	
	/**
	 * Simple map of String names and values, keeping their type by applying {@link #properlyCast(String)} method.
	 */
	Map<String, Object> properties = new HashMap<>();
	
	/**
	 * Loads given arguments to {@link #properties} map. Each argument must begin with "--", and values can be pointed either by "=" or single space (i.e. --argument=value or --argument value).
	 */
	static void loadCmdLnArgs(String... args) {
		Stream.of(String.join(" ", args).split("--")).skip(1).forEach(s -> {
			val parts = s.strip().split("[\\s=]", 2);
			Object value = null;
			try {
				value = properlyCast(parts[1]);
			} catch (IndexOutOfBoundsException ignored) {
			}
			properties.put(parts[0], value);
		});
	}
	
	/**
	 * Loads properties from given path. Uses {@link Properties} to parse.
	 */
	static void loadAppProperties(String propertiesPath) {
		Tools.getResource(propertiesPath).ifPresent(
				src -> {
					try (InputStream source = src.openStream()) {
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
					} catch (IOException | IllegalArgumentException ignored) {
					}
				}
		);
	}
	
	/**
	 * Loads cmd-line args and app properties together.
	 *
	 * @see #loadCmdLnArgs(String...)
	 * @see #loadAppProperties(String)
	 */
	static void load(String propertiesPath, String... args) {
		loadAppProperties(propertiesPath);
		loadCmdLnArgs(args);
	}
	
	/**
	 * Tries to cast the String to Integer, Long, Double or Boolean
	 */
	private static Object properlyCast(String arg) {
		try {
			return Integer.parseInt(arg);
		} catch (Exception ignored) {
		}
		
		try {
			return Long.parseLong(arg);
		} catch (Exception ignored) {
		}
		
		try {
			return Double.parseDouble(arg);
		} catch (Exception ignored) {
		}
		
		return switch (arg) {
			case "y", "n", "yes", "no", "true", "false" -> Boolean.parseBoolean(arg);
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
	
	static PropertiesInterface of(String name) {
		return () -> name;
	}
	
	/**
	 * @return Optional property's value.
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