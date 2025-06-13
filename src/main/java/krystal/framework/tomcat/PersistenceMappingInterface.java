package krystal.framework.tomcat;

import krystal.framework.database.persistence.Entity;
import lombok.NonNull;

public interface PersistenceMappingInterface extends CountableMappingInterface {
	
	@NonNull
	Class<? extends Entity> getPersistenceClass();
	
}