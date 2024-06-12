package krystal.framework.database.abstraction;

import krystal.framework.database.queryfactory.*;

import java.util.Collection;
import java.util.Optional;

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
	default ColumnsPairingInterface is(Object... values) {
		return ColumnIsPair.of(this, ColumnOperators.In, ColumnIsPair.parseValues(values));
	}
	
	/**
	 * @implNote To check for {@code NULL} either leave the list empty or type in {@code "null"}.
	 */
	default ColumnsPairingInterface isNot(Object... values) {
		return ColumnIsPair.of(this, ColumnOperators.notIn, ColumnIsPair.parseValues(values));
	}
	
	default ColumnsPairingInterface isBetween(Object left, Object right) {
		return ColumnIsPair.of(this, ColumnOperators.Between, ColumnIsPair.parseValues(left, right));
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
	
	static Optional<ColumnInterface> pickEqual(ColumnInterface column, Collection<ColumnInterface> from) {
		return from.stream()
		           .filter(c -> c.getSqlName().equalsIgnoreCase(column.getSqlName()))
		           .findFirst();
	}
	
	enum Functions {
		SUM, MIN, MAX, AVG, COUNT, TRIM
	}
	
}