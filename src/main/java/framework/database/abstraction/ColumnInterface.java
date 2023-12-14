package framework.database.abstraction;

import framework.database.queryfactory.*;

/**
 * Represents the database column instance. Attach to enum to create a convenient column access when building queries.
 */
@FunctionalInterface
public interface ColumnInterface {
	
	static ColumnInterface of(String name) {
		return () -> name;
	}
	
	String sqlName();
	
	default ColumnsPairingInterface is(Object... values) {
		return ColumnIsPair.of(this, ColumnOperators.In, ColumnIsPair.parseValues(values));
	}
	
	default ColumnsPairingInterface isNot(Object... values) {
		return ColumnIsPair.of(this, ColumnOperators.notIn, ColumnIsPair.parseValues(values));
	}
	
	default ColumnsPairingInterface is(ColumnOperators operator, Object to) {
		return ColumnIsPair.of(this, operator, ColumnIsPair.parseValues(to));
	}
	
	default ColumnsPairingInterface set(Object value) {
		return ColumnSetPair.of(this, value);
	}
	
	default ColumnsPairingInterface is(ColumnOperators operator, ColumnInterface to) {
		return ColumnPair.of(this, operator, to);
	}
	
	default ColumnsPairingInterface is(ColumnInterface equalTo) {
		return ColumnPair.of(this, ColumnOperators.Equal, equalTo);
	}
	
	default ColumnInterface as(String alias) {
		return () -> "%s %s".formatted(sqlName(), alias);
	}
	
	default ColumnInterface from(String alias) {
		return () -> "%s.%s".formatted(alias, sqlName());
	}
	
	default ColumnInterface sum() {
		return () -> "SUM(%s)".formatted(sqlName());
	}
	
	default ColumnInterface min() {
		return () -> "MIN(%s)".formatted(sqlName());
	}
	
	default ColumnInterface max() {
		return () -> "MAX(%s)".formatted(sqlName());
	}
	
	default ColumnInterface dist() {
		return () -> "DISTINCT " + sqlName();
	}
	
}