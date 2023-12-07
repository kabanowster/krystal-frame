package framework.database.implementation;

import framework.database.abstraction.JDBCDriverInterface;
import framework.database.abstraction.ProviderInterface;

/**
 * Default providers. Create your own using {@link ProviderInterface}.
 */
public enum Providers implements ProviderInterface {
	sqlserver(JDBCDrivers.sqlserver),
	as400(JDBCDrivers.as400);
	
	final JDBCDriverInterface driver;
	
	Providers(JDBCDriverInterface driver) {
		this.driver = driver;
	}
	
	@Override
	public JDBCDriverInterface jdbcDriver() {
		return driver;
	}
}