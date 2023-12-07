package framework.database.queryfactory;

interface WhereClauseInterface {
	
	Query pack();
	
	default WhereClause where(WhereClauseDelimiter delimiter, ColumnsPairingInterface... columnsAre) {
		return new WhereClause(pack(), delimiter, columnsAre);
	}
	
	default WhereClause where(ColumnsPairingInterface... columnsAre) {
		return new WhereClause(pack(), WhereClauseDelimiter.OR, columnsAre);
	}
	
}