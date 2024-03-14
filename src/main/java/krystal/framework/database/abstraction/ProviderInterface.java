package krystal.framework.database.abstraction;

import krystal.framework.database.implementation.DBCDrivers;

/**
 * Each provider references different database connection. {@link DBCDrivers DBCDrivers} is the list of default supported drivers. The properties for connection to each Provider are set within <i><b>"provider_name.properties"</b></i> file. The
 * <b><i>host</i></b> and <b><i>database</i></b> properties are mandatory. You can create as many providers as you want, with different properties and drivers.
 *
 * @see QueryExecutorInterface
 * @see QueryExecutorInterface.MandatoryProperties
 * @see DBCDriverInterface
 */
@FunctionalInterface
public interface ProviderInterface {
	
	DBCDriverInterface dbcDriver();
	
	default boolean equals(ProviderInterface provider) {
		return this.dbcDriver().getConnectionStringBase().equals(provider.dbcDriver().getConnectionStringBase());
	}
	
}