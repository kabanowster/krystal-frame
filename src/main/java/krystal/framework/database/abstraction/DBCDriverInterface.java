package krystal.framework.database.abstraction;

import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.database.implementation.QueryExecutor;
import krystal.framework.database.queryfactory.QueryType;

import java.util.Set;

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
public interface DBCDriverInterface {
	
	/**
	 * Base element of the default driver's connection string. I.e. {@code jdbc:sqlserver://}, {@code r2dbc:h2:}, etc.
	 */
	String getConnectionStringBase();
	
	Set<QueryType> getSupportedOutputtingStatements();
	
	default String getDriverName() {
		return getConnectionStringBase().split(":")[1];
	}
	
}