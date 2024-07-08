package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

/**
 * Pairing column with value for SET in UPDATE statement.
 */
public record ColumnSetValueComparison(ColumnInterface column, Object value) implements ColumnsComparisonInterface {
	
	@Override
	public String getComparison() {
		return String.format("%s = %s", column.getSqlName(), Query.parseValueForSQL(value));
	}
	
	@Override
	public String toString() {
		return getComparison();
	}
	
	public static ColumnSetValueComparison of(ColumnInterface column, Object value) {
		return new ColumnSetValueComparison(column, value);
	}
	
}