package krystal.framework.database.abstraction;

import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.queryfactory.*;
import lombok.val;

import java.util.stream.Stream;

/**
 * Represents the database table instance. Attach to enum to create a convenient query factory out of it.
 *
 * @see PersistenceInterface#getTable()
 */
@FunctionalInterface
public interface TableInterface {
	
	static TableInterface of(String name) {
		return () -> name;
	}
	
	/*
	 * Basic queries wrappers.
	 */
	
	/**
	 * Code custom renaming (i.e. prefix/suffix, brackets) or return <b><i>.toString()</i></b>.
	 */
	String sqlName();
	
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
	
	default TableInterface as(String alias) {
		return () -> "%s %s".formatted(sqlName(), alias);
	}
	
	default TableInterface join(JoinTypes joinType, TableInterface table, ColumnsPairingInterface... on) {
		return () -> {
			val result = new StringBuilder("%s %s JOIN %s ON 1=1".formatted(sqlName(), joinType, table.sqlName()));
			Stream.of(on).forEach(cpi -> result.append(" AND ").append(cpi.pairTogether()));
			return result.toString();
		};
	}
	
}