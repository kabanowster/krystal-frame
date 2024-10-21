package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.queryfactory.QueryType;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Set;

/**
 * Default implementations of standard DBC drivers.
 */
@Log4j2
@Getter
public enum DBCDrivers implements DBCDriverInterface {
	jdbcSQLServer("jdbc:sqlserver://", QueryType.CUDs()),
	jdbcAS400("jdbc:as400://", QueryType.INSERT),
	jdbcH2("jdbc:h2:"),
	jdbcPostgresql("jdbc:postgresql://"),
	jdbcMySQL("jdbc:mysql://");
	
	private final String connectionStringBase;
	private final Set<QueryType> supportedOutputtingStatements;
	
	DBCDrivers(String connectionStringBase, QueryType... supportedOutputtingStatements) {
		this.connectionStringBase = connectionStringBase;
		this.supportedOutputtingStatements = Set.of(supportedOutputtingStatements);
	}
}