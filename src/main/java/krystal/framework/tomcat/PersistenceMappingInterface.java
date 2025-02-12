package krystal.framework.tomcat;

import krystal.framework.database.persistence.Entity;
import lombok.NonNull;

public interface PersistenceMappingInterface {
	
	@NonNull
	Class<? extends Entity> getPersistenceClass();
	
	String name();
	
	default String mapping() {
		return "/" + name();
	}
	
	default String single() {
		return mapping() + "/*";
	}
	
	default String plural() {
		return switch (name().charAt(name().length() - 1)) {
			case 's' -> mapping() + "es";
			case 'y' -> mapping().substring(0, mapping().length() - 1) + "ies";
			default -> mapping() + "s";
		};
	}
	
	default boolean matches(String pattern) {
		if (pattern == null) return false;
		return pattern.equalsIgnoreCase(mapping())
				       || pattern.equalsIgnoreCase(single())
				       || pattern.equalsIgnoreCase(plural());
	}
	
}