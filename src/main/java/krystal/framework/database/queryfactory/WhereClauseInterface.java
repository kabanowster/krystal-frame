package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.Query;

interface WhereClauseInterface {
	
	Query pack();
	
	default WhereClause where(WhereClauseDelimiter delimiter, ColumnsPairingInterface... columnsAre) {
		return new WhereClause(pack(), delimiter, columnsAre);
	}
	
	/**
	 * By default, the values are linked with AND keyword.
	 */
	default WhereClause where(ColumnsPairingInterface... columnsAre) {
		return new WhereClause(pack(), WhereClauseDelimiter.AND, columnsAre);
	}
	
}