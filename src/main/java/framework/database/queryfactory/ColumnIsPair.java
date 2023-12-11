package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;
import krystal.Tools;

import java.util.List;

/**
 * Pairing column with values for different data comparisons in WHERE clauses.
 */
public record ColumnIsPair(ColumnInterface column, ColumnOperators operator, List<Object> values) implements ColumnsPairingInterface {
	
	public static List<Object> parseValues(Object... values) {
		return Query.parseValuesForSQL(values).filter(val -> !"NULL".equals(val) && val != null).toList();
	}
	
	@Override
	public String pairTogether() {
		boolean nullValue = values().isEmpty();
		return String.format(
				"%s%s %s " + (nullValue ? "%s" : "(%s)"),
				operator.prefix,
				column.sqlName(),
				nullValue ? "IS" : operator.face,
				switch (operator) {
					case In, notIn -> nullValue ? "NULL" : Tools.concat(KrystalFramework.getDefaultDelimeter(), values().stream());
					default -> values().stream().findFirst().get();
				}
		);
	}
	
	@Override
	public String toString() {
		return pairTogether();
	}
	
}