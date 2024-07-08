package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.Query;
import krystal.framework.database.implementation.Q;

interface WhereClauseInterface {
	
	Query pack();
	
	default WhereClause where(WhereClauseDelimiter delimiter, ColumnsComparisonInterface... columnsAre) {
		return new WhereClause(pack(), delimiter, columnsAre);
	}
	
	/**
	 * By default, the values are linked with AND keyword.
	 */
	default WhereClause where(ColumnsComparisonInterface... columnsAre) {
		return new WhereClause(pack(), WhereClauseDelimiter.AND, columnsAre);
	}
	
	/**
	 * 1=1
	 */
	default WhereClause where1is1() {
		return new WhereClause(pack(), WhereClauseDelimiter.NULL, Q.c("1").is(1));
	}
	
}