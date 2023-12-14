package framework.database.abstraction;

import framework.database.implementation.JDBCDrivers;
import framework.database.queryfactory.QueryType;
import framework.logging.LoggingInterface;

import java.util.List;
import java.util.Optional;

/**
 * SQL statement to be executed. {@link ProviderInterface} describes a database to run the query on. SELECT and INSERT queries are treated as reading - they return a {@link QueryResultInterface} which can then be processed. UPDATE and DELETE statements
 * produce an Integer value - the number of records being affected.
 */
public interface QueryInterface extends LoggingInterface {
	
	String sqlQuery();
	
	ProviderInterface getProvider();
	
	void setProvider(ProviderInterface provider);
	
	QueryType getType();
	
	default QueryResultInterface execute(QueryExecutorInterface executor) {
		QueryType type = Optional.ofNullable(getType()).orElse(determineType());
		switch (type) {
			case SELECT -> {
				return executor.read(this);
			}
			case INSERT -> {
				// drivers which return inserted rows as result
				if (List.of(
						JDBCDrivers.as400.asProvider(),
						JDBCDrivers.sqlserver.asProvider()
				).contains(getProvider()))
					return executor.read(this);
			}
		}
		
		return QueryResultInterface.singleton(() -> "#", executor.write(this));
	}
	
	private QueryType determineType() {
		String query = sqlQuery(); // unpacking!
		for (QueryType type : QueryType.values())
			if (query.matches(String.format("^%s[\\w\\W]*", type.toString()))) return type;
		
		log().warn("  ! UNDEFINED QueryType.");
		return QueryType.UNDEFINED;
	}
	
}