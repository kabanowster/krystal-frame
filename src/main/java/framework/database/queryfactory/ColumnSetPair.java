package framework.database.queryfactory;

import framework.database.abstraction.ColumnInterface;

/**
 * Pairing column with value for SET in UPDATE statement.
 */
public record ColumnSetPair(ColumnInterface column, Object value) implements ColumnsPairingInterface {
	
	@Override
	public String pairTogether() {
		return String.format("%s = %s", column.sqlName(), Query.parseValueForSQL(value));
	}
	
	@Override
	public String toString() {
		return pairTogether();
	}
	
	public static ColumnSetPair of(ColumnInterface column, Object value) {
		return new ColumnSetPair(column, value);
	}
	
}