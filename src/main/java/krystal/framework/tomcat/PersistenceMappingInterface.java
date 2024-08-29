package krystal.framework.tomcat;

import krystal.framework.database.persistence.ImportantPersistenceInterface;
import lombok.NonNull;

public interface PersistenceMappingInterface {
	
	@NonNull
	Class<? extends ImportantPersistenceInterface> getPersistenceClass();
	
	/**
	 * @apiNote The mapping name is converted to lower-case!
	 */
	String name();
	
	default String mapping() {
		return "/" + name().toLowerCase();
	}
	
	default String single() {
		return mapping() + "/*";
	}
	
	default String plural() {
		return mapping() + "s";
	}
	
}