package krystal.framework.database.queryfactory;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Pairing column with values for different data comparisons in WHERE clauses.
 */
public record ColumnToValueComparison(ColumnInterface column, ComparisonOperator is, List<Object> values) implements ColumnsComparisonInterface {
	
	public ColumnToValueComparison {
		values = Query.parseValuesForSQL(values.toArray()).filter(val -> val != null && !"null".equalsIgnoreCase(String.valueOf(val))).toList();
	}
	
	public ColumnToValueComparison(ColumnInterface column, ComparisonOperator is, Object... values) {
		this(column, is, Arrays.stream(values).filter(Objects::nonNull).toList());
	}
	
	@Override
	public String getComparison() {
		boolean nullValue = values.isEmpty();
		return String.format(
				"%s%s %s " + (nullValue || is.equals(ComparisonOperator.BETWEEN) ? "%s" : "(%s)"),
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
	
	public static ColumnToValueComparison of(ColumnInterface column, ComparisonOperator operator, List<Object> values) {
		return new ColumnToValueComparison(column, operator, values);
	}
	
	public static ColumnToValueComparison of(ColumnInterface column, ComparisonOperator operator, Object value) {
		return new ColumnToValueComparison(column, operator, List.of(value));
	}
	
	public static ColumnToValueComparison of(ColumnInterface column, ComparisonOperator operator, Object[] values) {
		return new ColumnToValueComparison(column, operator, Arrays.stream(values).toList());
	}
	
}