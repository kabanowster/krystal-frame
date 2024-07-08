package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;

public record ColumnToColumnComparison(ColumnInterface first, ColumnsComparisonOperator is, ColumnInterface second) implements ColumnsComparisonInterface {
	
	@Override
	public String getComparison() {
		return "%s %s %s".formatted(first.getSqlName(), is.face, second.getSqlName());
	}
	
	@Override
	public String toString() {
		return getComparison();
	}
	
	public static ColumnToColumnComparison of(ColumnInterface first, ColumnsComparisonOperator is, ColumnInterface second) {
		return new ColumnToColumnComparison(first, is, second);
	}
	
}