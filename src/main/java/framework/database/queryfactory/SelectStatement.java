package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;
import framework.database.implementation.JDBCDrivers;
import framework.logging.LoggingInterface;

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
	public SelectStatement providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build() {
		if (from == null || query != null)
			return;
		
		// TODO last if negative
		var limitString = "";
		if (limit > 0) {
			switch (provider.jdbcDriver()) {
				case JDBCDrivers.sqlserver -> limitString = " TOP " + limit;
				case JDBCDrivers.as400 -> appendLast.add("\nFETCH %s FIRST ROWS ONLY".formatted(limit));
				default -> log().warn("  ! Unsupported LIMIT keyword in select statement for %s provider.".formatted(provider));
			}
		}
		
		//@formatter:off
		query = new StringBuilder(
				String.format(
						"SELECT%s%s %s FROM %s",
						distinct ? " DISTINCT" : "",
						limitString,
						!columns.isEmpty() ? columns.stream().map(ColumnInterface::sqlName).collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())) : "*",
						from.sqlName()
				));
		//@formatter:on
	}
	
}