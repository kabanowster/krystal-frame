package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.logging.LoggingInterface;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SelectStatement extends Query implements WhereClauseInterface, OrderByInterface, GroupByInterface, LoggingInterface {
	
	private final Set<ColumnInterface> columns;
	private TableInterface from;
	private int limit;
	private boolean distinct;
	
	public SelectStatement() {
		super(QueryType.SELECT);
		columns = Collections.synchronizedSet(new LinkedHashSet<>());
	}
	
	public SelectStatement(TableInterface from) {
		this();
		this.from = from;
	}
	
	public SelectStatement(ColumnInterface... columns) {
		this();
		this.columns.addAll(Stream.of(columns).toList());
	}
	
	public SelectStatement(TableInterface from, ColumnInterface... columns) {
		this(from);
		this.columns.addAll(Stream.of(columns).toList());
	}
	
	public static SelectStatement columns(ColumnInterface... columns) {
		return new SelectStatement(columns);
	}
	
	public SelectStatement from(TableInterface from) {
		this.from = from;
		return this;
	}
	
	public SelectStatement limit(int limit) {
		this.limit = limit;
		return this;
	}
	
	public SelectStatement distinct() {
		distinct = true;
		return this;
	}
	
	@Override
	public SelectStatement setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (from == null)
			throw new IllegalArgumentException();
		
		// TODO LAST if negative
		var limitString = "";
		if (limit > 0) {
			// val drv = provider.dbcDriver();
			switch (provider.dbcDriver()) {
				case DBCDrivers.jdbcSQLServer, DBCDrivers.r2dbcSQLServer -> limitString = " TOP " + limit;
				case DBCDrivers.jdbcMySQL, DBCDrivers.r2dbcMySQL -> appendLast.add("LIMIT " + limit);
				default -> appendLast.add("FETCH FIRST %s ROWS ONLY".formatted(limit));
			}
			//
			//
			// if (DBCDrivers.jdbcSQLServer.equals(drv)
			// 		|| DBCDrivers.r2dbcSQLServer.equals(drv))
			// 	limitString = " TOP " + limit;
			// else if (DBCDrivers.jdbcMySQL.asProvider().equals(provider)
			// 		|| DBCDrivers.r2dbcMySQL.asProvider().equals(provider))
			// 	appendLast.add("LIMIT " + limit);
			// else appendLast.add("FETCH FIRST %s ROWS ONLY".formatted(limit));
			// else log().warn("  ! Unsupported LIMIT keyword in select statement for %s provider.".formatted(provider));
		}
		
		query.append(
				String.format(
						"SELECT%s%s %s FROM %s",
						distinct ? " DISTINCT" : "",
						limitString,
						!columns.isEmpty() ? columns.stream().map(ColumnInterface::sqlName).collect(Collectors.joining(", ")) : "*",
						from.sqlName()
				));
	}
	
}