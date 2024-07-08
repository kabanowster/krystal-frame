package krystal.framework.database.queryfactory;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.List;
import java.util.stream.Stream;

/**
 * Pairing column with values for different data comparisons in WHERE clauses.
 */
public record ColumnToValueComparison(ColumnInterface column, ColumnsComparisonOperator is, List<Object> values) implements ColumnsComparisonInterface {
	
	public ColumnToValueComparison {
		values = parseValues(values.toArray());
	}
	
	public static List<Object> parseValues(Object... values) {
		return Query.parseValuesForSQL(values).filter(val -> !"NULL".equals(val) && val != null).toList();
	}
	
	@Override
	public String getComparison() {
		boolean nullValue = values.isEmpty();
		return String.format(
				"%s%s %s " + (nullValue || is.equals(ColumnsComparisonOperator.BETWEEN) ? "%s" : "(%s)"),
				is.prefix,
				column.getSqlName(),
				nullValue ? "IS" : is.face,
				switch (is) {
					case IN, NOT_IN -> nullValue ? "NULL" : Tools.concat(KrystalFramework.getDefaultDelimeter(), values.stream());
					case EQUAL, NOT_EQUAL -> nullValue ? "NULL" : values.getFirst();
					case BETWEEN -> "%s AND %s".formatted(values.getFirst(), values.getLast());
					default -> values.getFirst();
				}
		);
	}
	
	@Override
	public String toString() {
		return getComparison();
	}
	
	public static ColumnToValueComparison of(ColumnInterface column, ColumnsComparisonOperator operator, List<Object> values) {
		return new ColumnToValueComparison(column, operator, values);
	}
	
	public static ColumnToValueComparison of(ColumnInterface column, ColumnsComparisonOperator operator, Object... values) {
		return new ColumnToValueComparison(column, operator, Stream.of(values).toList());
	}
	
}