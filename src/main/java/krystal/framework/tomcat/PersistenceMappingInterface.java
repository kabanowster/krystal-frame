package krystal.framework.tomcat;

import krystal.framework.database.persistence.ImportantPersistenceInterface;

public interface PersistenceMappingInterface {
	
	Class<? extends ImportantPersistenceInterface> getPersistenceClass();
	
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