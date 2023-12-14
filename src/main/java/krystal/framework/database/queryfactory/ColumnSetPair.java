package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

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