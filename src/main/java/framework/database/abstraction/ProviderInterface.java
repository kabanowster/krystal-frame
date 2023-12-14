package framework.database.abstraction;

import framework.database.implementation.JDBCDrivers;

/**
 * Each provider references different database connection. {@link JDBCDrivers JDBCDrivers} is the list of default supported drivers. The properties for connection to each Provider are set within <i><b>"provider_name.properties"</b></i> file. The
 * <b><i>server</i></b> and <b><i>database</i></b> properties are mandatory, as others within
 * {@link QueryExecutorInterface.MandatoryProperties MandatoryProperties}. You can create as many providers as you want, with different properties and drivers.
 *
 * @see QueryExecutorInterface.MandatoryProperties
 * @see JDBCDriverInterface
 */
@FunctionalInterface
public interface ProviderInterface {
	
	JDBCDriverInterface jdbcDriver();
	
}