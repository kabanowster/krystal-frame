package krystal.framework.database.queryfactory;

import krystal.Tools;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.implementation.DBCDrivers;
import lombok.val;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpdateStatement extends Query implements WhereClauseInterface {
	
	private final Set<ColumnsComparisonInterface> columnSetPairs;
	private final TableInterface table;
	private final Set<ColumnInterface> output;
	
	public UpdateStatement(TableInterface table) {
		super(QueryType.UPDATE);
		columnSetPairs = Collections.synchronizedSet(new HashSet<>());
		output = Collections.synchronizedSet(new LinkedHashSet<>());
		this.table = table;
	}
	
	public UpdateStatement(TableInterface table, ColumnsComparisonInterface... columnSetPairs) {
		this(table);
		this.columnSetPairs.addAll(Stream.of(columnSetPairs).toList());
	}
	
	public static UpdateStatement table(TableInterface table) {
		return new UpdateStatement(table);
	}
	
	public UpdateStatement set(ColumnsComparisonInterface... columnSetPairs) {
		this.columnSetPairs.addAll(Stream.of(columnSetPairs).toList());
		return this;
	}
	
	public UpdateStatement output(ColumnInterface... columns) {
		output.addAll(Stream.of(columns).toList());
		return this;
	}
	
	@Override
	public UpdateStatement setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (table == null || columnSetPairs.isEmpty())
			throw new IllegalArgumentException();
		
		query.append("UPDATE ").append(table.getSqlName());
		query.append(" SET ").append(Tools.concat(", ", columnSetPairs.stream()));
		
		/*
		 * Output updated
		 */
		
		val drv = provider.getDriver();
		if (DBCDrivers.jdbcSQLServer.equals(drv)) {
			query.append(String.format(
					" OUTPUT %s",
					output.isEmpty() ? "INSERTED.*" :
					output.stream()
					      .map(c -> "INSERTED." + c.getSqlName())
					      .collect(Collectors.joining(", "))
			));
		}
		
		// Not supported in DB2-i
		// if (DBCDrivers.jdbcAS400.equals(provider.getDriver())) {
		// 	query.replace(0, query.length(), String.format(
		// 			"SELECT %s FROM FINAL TABLE (%s)",
		// 			output.isEmpty() ? "*" :
		// 			output.stream()
		// 			      .map(ColumnInterface::getSqlName)
		// 			      .collect(Collectors.joining(", ")),
		// 			query
		// 	));
		// }
	}
	
	// TODO UPDATE from SELECT. Create "from" chain.
}