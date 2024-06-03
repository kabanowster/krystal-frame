package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;

/**
 * Default providers. Create your own using {@link ProviderInterface}.
 */
public enum DefaultProviders implements ProviderInterface {
	sqlserver(DBCDrivers.jdbcSQLServer),
	@Deprecated
	r2sqlserver(DBCDrivers.r2dbcSQLServer),
	as400(DBCDrivers.jdbcAS400),
	h2(DBCDrivers.jdbcH2),
	@Deprecated
	r2h2(DBCDrivers.r2dbcH2),
	postgre(DBCDrivers.jdbcPostgresql),
	@Deprecated
	r2postgre(DBCDrivers.r2dbcPostgresql),
	mysql(DBCDrivers.jdbcMySQL),
	@Deprecated
	r2mysql(DBCDrivers.r2dbcMySQL);
	
	private final DBCDriverInterface driver;
	
	DefaultProviders(DBCDriverInterface driver) {
		this.driver = driver;
	}
	
	@Override
	public DBCDriverInterface dbcDriver() {
		return driver;
	}
}