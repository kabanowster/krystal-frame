package krystal.framework.database.abstraction;

import krystal.framework.KrystalFramework;
import krystal.framework.database.implementation.DBCDrivers;

/**
 * Each provider references different database connector. {@link DBCDrivers DBCDrivers} is the list of default supported drivers (you can set your own drivers as well). The properties for connection to each Provider are set within
 * <i><b>"provider_name.properties"</b></i> file. The
 * <b><i>host</i></b> and <b><i>database</i></b> properties are mandatory. You can create as many providers as you want, with different properties and drivers.
 *
 * @see KrystalFramework#getDefaultProvider()
 * @see KrystalFramework#getProvidersPropertiesDir()
 * @see QueryExecutorInterface
 * @see QueryExecutorInterface.MandatoryProperties
 * @see DBCDriverInterface
 */
public interface ProviderInterface {
	
	String name();
	
	DBCDriverInterface dbcDriver();
	
	default boolean equals(ProviderInterface provider) {
		return this.dbcDriver().getConnectionStringBase().equals(provider.dbcDriver().getConnectionStringBase());
	}
	
}