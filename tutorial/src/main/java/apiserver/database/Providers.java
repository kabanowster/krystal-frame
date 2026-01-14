package apiserver.database;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.implementation.DBCDrivers;
import lombok.Getter;

@Getter
public enum Providers implements ProviderInterface {
	primary(DBCDrivers.jdbcSQLServer),
	secondary(DBCDrivers.jdbcPostgresql);
	
	private final DBCDriverInterface driver;
	
	Providers(DBCDriverInterface driver) {
		this.driver = driver;
	}
}