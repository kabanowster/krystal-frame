package framework.database.abstraction;

import framework.database.queryfactory.*;

/**
 * Represents the database table instance. Attach to enum to create a convenient query factory out of it.
 */
@FunctionalInterface
public interface TableInterface {
	
	/**
	 * Code custom renaming (i.e. prefix/suffix, brackets) or return <b><i>.toString()</i></b>.
	 */
	String sqlName();
	
	/*
	 * Basic queries wrappers.
	 */
	
	default SelectStatement select(ColumnInterface... columns) {
		return new SelectStatement(this, columns);
	}
	
	default UpdateStatement update(ColumnsPairingInterface... columnSetPairs) {
		return new UpdateStatement(this, columnSetPairs);
	}
	
	default InsertStatement insert(ColumnInterface... columns) {
		return new InsertStatement(this, columns);
	}
	
	default DeleteStatement delete() {
		return new DeleteStatement(this);
	}
	
}