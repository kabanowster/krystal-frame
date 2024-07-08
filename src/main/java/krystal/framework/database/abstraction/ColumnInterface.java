package krystal.framework.database.abstraction;

import krystal.framework.database.queryfactory.*;

import java.util.List;

/**
 * Represents the database column instance. Attach to enum to create a convenient column access when building queries.
 */
@FunctionalInterface
public interface ColumnInterface {
	
	static ColumnInterface of(String name) {
		return () -> name;
	}
	
	String getSqlName();
	
	/**
	 * @implNote To check for {@code NULL} either leave the list empty or type in {@code "null"}.
	 */
	default ColumnsComparisonInterface is(Object... values) {
		return ColumnToValueComparison.of(this, ColumnsComparisonOperator.IN, values);
	}
	
	/**
	 * @implNote To check for {@code NULL} either leave the list empty or type in {@code "null"}.
	 */
	default ColumnsComparisonInterface isNot(Object... values) {
		return ColumnToValueComparison.of(this, ColumnsComparisonOperator.NOT_IN, values);
	}
	
	default ColumnsComparisonInterface isBetween(Object left, Object right) {
		return ColumnToValueComparison.of(this, ColumnsComparisonOperator.BETWEEN, List.of(left, right));
	}
	
	default ColumnsComparisonInterface is(ColumnsComparisonOperator operator, Object to) {
		return ColumnToValueComparison.of(this, operator, to);
	}
	
	default ColumnsComparisonInterface set(Object value) {
		return ColumnSetValueComparison.of(this, value);
	}
	
	default ColumnsComparisonInterface is(ColumnsComparisonOperator operator, ColumnInterface to) {
		return ColumnToColumnComparison.of(this, operator, to);
	}
	
	default ColumnsComparisonInterface is(ColumnInterface equalTo) {
		return ColumnToColumnComparison.of(this, ColumnsComparisonOperator.EQUAL, equalTo);
	}
	
	default ColumnInterface as(String alias) {
		return () -> "%s %s".formatted(getSqlName(), alias);
	}
	
	default ColumnInterface from(String tableAlias) {
		return () -> "%s.%s".formatted(tableAlias, getSqlName());
	}
	
	default ColumnInterface dist() {
		return () -> "DISTINCT " + getSqlName();
	}
	
	default ColumnInterface fun(Functions function) {
		return fun(function.name(), true);
	}
	
	default ColumnInterface fun(Functions function, boolean addNameAsAlias) {
		return fun(function.name(), addNameAsAlias);
	}
	
	default ColumnInterface fun(String function, boolean addNameAsAlias) {
		return () -> "%s(%s)%s".formatted(function, getSqlName(), addNameAsAlias ? " " + getSqlName() : "");
	}
	
	enum Functions {
		SUM, MIN, MAX, AVG, COUNT, TRIM
	}
	
}