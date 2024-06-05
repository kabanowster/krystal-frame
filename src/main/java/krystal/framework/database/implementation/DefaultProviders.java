package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;

/**
 * Default providers. Create your own using {@link ProviderInterface}.
 */
public enum DefaultProviders implements ProviderInterface {
	sqlserver(DBCDrivers.jdbcSQLServer),
	as400(DBCDrivers.jdbcAS400),
	h2(DBCDrivers.jdbcH2),
	postgres(DBCDrivers.jdbcPostgresql),
	mysql(DBCDrivers.jdbcMySQL);
	private final DBCDriverInterface driver;
	
	DefaultProviders(DBCDriverInterface driver) {
		this.driver = driver;
	}
	
	@Override
	public DBCDriverInterface dbcDriver() {
		return driver;
	}
}