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
public record ColumnIsPair(ColumnInterface column, ColumnOperators operator, List<Object> values) implements ColumnsPairingInterface {
	
	public static List<Object> parseValues(Object... values) {
		return Query.parseValuesForSQL(values).filter(val -> !"NULL".equals(val) && val != null).toList();
	}
	
	@Override
	public String pairTogether() {
		boolean nullValue = values.isEmpty();
		return String.format(
				"%s%s %s " + (nullValue || operator.equals(ColumnOperators.Between) ? "%s" : "(%s)"),
				operator.prefix,
				column.sqlName(),
				nullValue ? "IS" : operator.face,
				switch (operator) {
					case In, notIn -> nullValue ? "NULL" : Tools.concat(KrystalFramework.getDefaultDelimeter(), values.stream());
					case Equal, notEqual -> nullValue ? "NULL" : values.getFirst();
					case Between -> "%s AND %s".formatted(values.getFirst(), values.getLast());
					default -> values.getFirst();
				}
		);
	}
	
	@Override
	public String toString() {
		return pairTogether();
	}
	
	public static ColumnIsPair of(ColumnInterface column, ColumnOperators operator, List<Object> values) {
		return new ColumnIsPair(column, operator, values);
	}
	
	public static ColumnIsPair of(ColumnInterface column, ColumnOperators operator, Object... values) {
		return new ColumnIsPair(column, operator, Stream.of(values).toList());
	}
	
}