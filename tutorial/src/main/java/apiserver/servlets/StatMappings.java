package apiserver.servlets;

import apiserver.entities.Option;
import apiserver.entities.Stat;
import apiserver.entities.StatsOption;
import krystal.framework.database.persistence.Entity;
import krystal.framework.tomcat.PersistenceMappingInterface;
import lombok.Getter;

@Getter
public enum StatMappings implements PersistenceMappingInterface {
	stat(Stat.class),
	option(Option.class),
	statsOption(StatsOption.class);
	
	private final Class<? extends Entity> persistenceClass;
	
	StatMappings(Class<? extends Entity> persistenceClass) {
		this.persistenceClass = persistenceClass;
	}
}