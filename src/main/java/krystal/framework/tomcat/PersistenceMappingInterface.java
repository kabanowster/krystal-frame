package krystal.framework.tomcat;

import krystal.framework.database.persistence.ImportantPersistenceInterface;
import lombok.NonNull;

public interface PersistenceMappingInterface {
	
	@NonNull
	Class<? extends ImportantPersistenceInterface> getPersistenceClass();
	
	String name();
	
	default String mapping() {
		return "/" + name();
	}
	
	default String single() {
		return mapping() + "/*";
	}
	
	default String plural() {
		return mapping() + "s";
	}
	
}