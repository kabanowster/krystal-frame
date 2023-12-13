package framework.database.abstraction;

import framework.database.queryfactory.QueryType;

import java.util.Objects;

/**
 * SQL statement to be executed. {@link ProviderInterface} describes a database to run the query on. SELECT and INSERT queries are treated as reading - they return a {@link QueryResultInterface} which can then be processed. UPDATE and DELETE statements
 * produce an Integer value - the number of records being affected.
 */
public interface QueryInterface {
	
	String sqlQuery();
	
	ProviderInterface getProvider();
	
	default QueryResultInterface execute(QueryExecutorInterface executor) {
		switch (Objects.requireNonNull(determineType())) {
			case SELECT, INSERT -> {
				return executor.read(this);
			}
			default -> {
				return QueryResultInterface.singleton(() -> "#", executor.write(this));
			}
		}
	}
	
	default QueryType determineType() {
		// too short for stream
		for (QueryType type : QueryType.values())
			if (sqlQuery().matches(String.format("^%s[\\w\\W]*", type.toString()))) return type;
		return null;
	}
	
}