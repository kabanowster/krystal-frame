package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupByStatement extends Query implements OrderByInterface {
	
	private final Set<ColumnInterface> columns = Collections.synchronizedSet(new LinkedHashSet<>());
	
	public GroupByStatement(Query query, ColumnInterface... columns) {
		super(query);
		columns(columns);
	}
	
	public GroupByStatement columns(ColumnInterface... columns) {
		this.columns.addAll(Stream.of(columns).toList());
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (type != QueryType.SELECT || columns.isEmpty())
			throw new IllegalArgumentException();
		
		query.append(String.format(" GROUP BY %s",
		                           columns.stream().map(ColumnInterface::getSqlName).collect(Collectors.joining(", "))));
	}
	
}