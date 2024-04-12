package krystal.framework.database.abstraction;

import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.database.implementation.DriverType;
import krystal.framework.database.implementation.QueryExecutor;
import lombok.val;

/**
 * Driver used to establish database connections. To implement your own driver:
 * <ol>
 *  <li>Add the driver package to the classpath.</li>
 *  <li>Implement this interface, setting the {@link #getConnectionStringBase()} as driver's default. I.e. {@code jdbc:sqlserver://}, {@code r2dbc:h2:}, etc.</li>
 *  <li>Use your driver to implement {@link ProviderInterface}.</li>
 *  <li>Load {@link ProviderInterface} with implementation {@link QueryExecutorInterface} (default: {@link QueryExecutor}).</li>
 * </ol>
 *
 * @see DBCDrivers
 */
@FunctionalInterface
public interface DBCDriverInterface {
	
	/**
	 * Base element of the default driver's connection string. I.e. {@code jdbc:sqlserver://}, {@code r2dbc:h2:}, etc.
	 */
	String getConnectionStringBase();
	
	default DriverType getDriverType() {
		for (val type : DriverType.values())
			if (getConnectionStringBase().contains(type.name())) return type;
		return DriverType.jdbc;
	}
	
	default String getDriverName() {
		return getConnectionStringBase().split(":")[1];
	}
	
}