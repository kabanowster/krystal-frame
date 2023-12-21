package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public enum DBCDrivers implements DBCDriverInterface {
	jdbcSQLServer("jdbc:sqlserver://"),
	r2dbcSQLServer("r2dbc:mssql://"),
	jdbcAS400("jdbc:as400://"),
	jdbcH2("jdbc:h2:"),
	r2dbcH2("r2dbc:h2:"),
	jdbcPostgresql("jdbc:postgresql://"),
	r2dbcPostgresql("r2dbc:postgresql://"),
	jdbcMySQL("jdbc:mysql://"),
	r2dbcMySQL("r2dbc:mysql://");
	
	private final String connectionStringBase;
	
	DBCDrivers(String connectionStringBase) {
		this.connectionStringBase = connectionStringBase;
	}
	
}