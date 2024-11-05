package krystal.framework.database.abstraction;

import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.queryfactory.*;
import lombok.NonNull;
import lombok.val;

/**
 * Represents the database table instance. Attach to {@link Enum} to create a convenient query factory out of it.
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
	String getSqlName();
	
	default SelectStatement select(ColumnInterface... columns) {
		return new SelectStatement(this, columns);
	}
	
	default UpdateStatement update(ColumnsComparisonInterface... columnSetPairs) {
		return new UpdateStatement(this, columnSetPairs);
	}
	
	default InsertStatement insert(ColumnInterface... columns) {
		return new InsertStatement(this, columns);
	}
	
	default DeleteStatement delete() {
		return new DeleteStatement(this);
	}
	
	default TableInterface as(String alias) {
		return () -> "%s %s".formatted(getSqlName(), alias);
	}
	
	default TableInterface join(JoinTypes joinType, TableInterface table, ColumnsComparisonInterface... on) {
		return () -> {
			val result = new StringBuilder("%s %s JOIN %s ON 1=1".formatted(getSqlName(), joinType, table.getSqlName()));
			for (var cpi : on)
				result.append(" AND ").append(cpi.getComparison());
			return result.toString();
		};
	}
	
	default TableInterface joinSelf(@NonNull String firstAlias, @NonNull String secondAlias, String... otherAliases) {
		return () -> {
			val n = getSqlName();
			val result = new StringBuilder("%s %s, %s %s".formatted(n, firstAlias, n, secondAlias));
			for (var alias : otherAliases)
				result.append(", ").append("%s %s".formatted(n, alias));
			return result.toString();
		};
	}
	
	default ColumnInterface col(String columnName) {
		return ColumnInterface.of(columnName).from(getSqlName());
	}
	
	default ColumnInterface col(ColumnInterface column) {
		return column.from(getSqlName());
	}
	
}