package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Default implementations of standard DBC drivers.
 */
@Log4j2
@Getter
public enum DBCDrivers implements DBCDriverInterface {
	jdbcSQLServer("jdbc:sqlserver://"),
	jdbcAS400("jdbc:as400://"),
	jdbcH2("jdbc:h2:"),
	jdbcPostgresql("jdbc:postgresql://"),
	jdbcMySQL("jdbc:mysql://");
	
	private final String connectionStringBase;
	
	DBCDrivers(String connectionStringBase) {
		this.connectionStringBase = connectionStringBase;
	}
	
}