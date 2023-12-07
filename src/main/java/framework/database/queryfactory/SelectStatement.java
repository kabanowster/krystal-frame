package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;
import framework.database.implementation.Providers;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SelectStatement extends Query implements WhereClauseInterface, OrderByInterface, GroupByInterface {
	
	private final Set<ColumnInterface> columns = Collections.synchronizedSet(new LinkedHashSet<>());
	private TableInterface from;
	private int limit;
	private boolean distinct;
	
	public SelectStatement(TableInterface from) {
		super(QueryType.SELECT);
		this.from = from;
	}
	
	public SelectStatement(TableInterface from, ColumnInterface... columns) {
		this(from);
		columns(columns);
	}
	
	// Setter dependency injection
	public SelectStatement from(TableInterface from) {
		this.from = from;
		return this;
	}
	
	public SelectStatement columns(ColumnInterface... columns) {
		this.columns.addAll(Stream.of(columns).toList());
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
		
		//@formatter:off
		query = new StringBuilder(
				String.format(
						"SELECT%s%s %s FROM %s",
						distinct ? " DISTINCT" : "",
						limit > 0 && Providers.sqlserver.equals(provider) ? " TOP " + limit : "",
						!columns.isEmpty() ? columns.stream().map(ColumnInterface::sqlName).collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())) : "*",
						from.sqlName()
				));
		//@formatter:on
	}
	
}