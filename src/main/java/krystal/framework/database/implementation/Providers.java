package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;

/**
 * Default providers. Create your own using {@link ProviderInterface}.
 */
public enum Providers implements ProviderInterface {
	sqlserver(DBCDrivers.jdbcSQLServer),
	r2sqlserver(DBCDrivers.r2dbcSQLServer);
	
	private final DBCDriverInterface driver;
	
	Providers(DBCDriverInterface driver) {
		this.driver = driver;
	}
	
	@Override
	public DBCDriverInterface dbcDriver() {
		return driver;
	}
}