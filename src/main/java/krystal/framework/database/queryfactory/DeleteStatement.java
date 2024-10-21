package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.implementation.DBCDrivers;
import lombok.val;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeleteStatement extends Query implements WhereClauseInterface {
	
	private final TableInterface from;
	private final Set<ColumnInterface> output;
	
	public DeleteStatement(TableInterface from) {
		super(QueryType.DELETE);
		output = Collections.synchronizedSet(new LinkedHashSet<>());
		this.from = from;
	}
	
	public static DeleteStatement from(TableInterface from) {
		return new DeleteStatement(from);
	}
	
	public DeleteStatement output(ColumnInterface... columns) {
		output.addAll(Stream.of(columns).toList());
		return this;
	}
	
	@Override
	public DeleteStatement setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (from == null)
			throw new IllegalArgumentException();
		
		query.append("DELETE FROM ").append(from.getSqlName());
		
		/*
		 * Output deleted
		 */
		
		val drv = provider.getDriver();
		if (DBCDrivers.jdbcSQLServer.equals(drv)) {
			query.append(String.format(
					" OUTPUT %s",
					output.isEmpty() ? "DELETED.*" :
					output.stream()
					      .map(c -> "DELETED." + c.getSqlName())
					      .collect(Collectors.joining(", "))
			));
		}
		
		// Not supported in DB2-i
		// if (DBCDrivers.jdbcAS400.equals(provider.getDriver())) {
		// 	query.replace(0, query.length(), String.format(
		// 			"SELECT %s FROM OLD TABLE (%s)",
		// 			output.isEmpty() ? "*" :
		// 			output.stream()
		// 			      .map(ColumnInterface::getSqlName)
		// 			      .collect(Collectors.joining(", ")),
		// 			query
		// 	));
		// }
		
	}
	
}