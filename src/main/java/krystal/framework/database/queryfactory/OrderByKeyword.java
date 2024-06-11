package krystal.framework.database.queryfactory;

import krystal.framework.KrystalFramework;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderByKeyword extends Query {
	
	private final Set<ColumnInterface> columns = Collections.synchronizedSet(new LinkedHashSet<>());
	private OrderByDirection order;
	
	public OrderByKeyword(Query query, ColumnInterface... columns) {
		super(query);
		columns(columns);
	}
	
	public OrderByKeyword(Query query, OrderByDirection order, ColumnInterface... columns) {
		this(query, columns);
		order(order);
	}
	
	public OrderByKeyword order(OrderByDirection direction) {
		order = direction;
		return this;
	}
	
	public OrderByKeyword columns(ColumnInterface... columns) {
		this.columns.addAll(Stream.of(columns).toList());
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (type != QueryType.SELECT || columns.isEmpty())
			throw new IllegalArgumentException();
		
		query.append(String.format(" ORDER BY %s%s",
		                           columns.stream().map(ColumnInterface::getSqlName).collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())),
		                           order != null ? " " + order : ""));
	}
	
}