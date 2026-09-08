package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import lombok.Getter;

/**
 * Default providers. Create your own using {@link ProviderInterface}.
 */
@Getter
public enum DefaultProviders implements ProviderInterface {
	sqlserver(DBCDrivers.jdbcSQLServer),
	as400(DBCDrivers.jdbcAS400);
	
	private final DBCDriverInterface driver;
	
	DefaultProviders(DBCDriverInterface driver) {
		this.driver = driver;
	}
}