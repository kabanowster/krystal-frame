package krystal.framework.database.abstraction;

import krystal.framework.database.implementation.DriverType;
import lombok.val;

public interface DBCDriverInterface {
	
	String getConnectionStringBase();
	
	default ProviderInterface asProvider() {
		return () -> this;
	}
	
	default DriverType getDriverType() {
		for (val type : DriverType.values())
			if (getConnectionStringBase().contains(type.name())) return type;
		return DriverType.jdbc;
	}
	
	default String getDriverName() {
		return getConnectionStringBase().split(":")[1];
	}
	
}