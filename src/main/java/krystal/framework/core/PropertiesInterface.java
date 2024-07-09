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
	 * Loads given arguments to {@link #properties} map. Each argument must begin with {@code --}, and values can be pointed either by {@code space}, {@code =} or {@code :} (i.e. --argument=value).
	 */
	static void loadCmdLnArgs(String... args) {
		Stream.of(String.join(" ", args).split("--")).skip(1).forEach(s -> {
			val parts = s.strip().split("[\\s=:]+", 2);
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
							} catch (NullPointerException _) {
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
		
		if (arg.matches("\\d*[,.]\\d+")) {
			return Double.parseDouble(arg.replace(",", "."));
		}
		
		if (arg.matches("\\d{1,9}")) {
			return Integer.parseInt(arg);
		}
		
		if (arg.matches("\\d+")) {
			return Long.parseLong(arg);
		}
		
		if (arg.matches("[yn]|yes|no|true|false")) {
			return Boolean.parseBoolean(arg);
		}
		
		return arg;
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
	 * @return True if the property has been listed.
	 */
	default boolean isPresent() {
		return properties.containsKey(name());
	}
	
	/**
	 * Equivalent of {@link Map#put(Object, Object)}, sets the value for this property.
	 */
	default Object set(Object value) {
		return properties.put(name(), value);
	}
	
	/**
	 * Does value casting before putting to the list.
	 *
	 * @see #properlyCast(String)
	 */
	default Object set(String value) {
		return set(properlyCast(value));
	}
	
}