package framework.database.abstraction;

import framework.database.queryfactory.ColumnIsPair;
import framework.database.queryfactory.ColumnOperators;
import framework.database.queryfactory.ColumnSetPair;
import framework.database.queryfactory.ColumnsPairingInterface;

/**
 * Represents the database column instance. Attach to enum to create a convenient column access when building queries.
 */
@FunctionalInterface
@SuppressWarnings("unchecked")
public interface ColumnInterface {
	
	String sqlName();
	
	default <T> ColumnsPairingInterface is(T... values) {
		return new ColumnIsPair(this, ColumnOperators.In, ColumnIsPair.parseValues(values));
	}
	
	default <T> ColumnsPairingInterface isNot(T... values) {
		return new ColumnIsPair(this, ColumnOperators.notIn, ColumnIsPair.parseValues(values));
	}
	
	default <T> ColumnsPairingInterface is(ColumnOperators operator, T value) {
		return new ColumnIsPair(this, operator, ColumnIsPair.parseValues(value));
	}
	
	default <T> ColumnsPairingInterface set(T value) {
		return new ColumnSetPair(this, value);
	}
	
}