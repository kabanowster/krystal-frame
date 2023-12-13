package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;

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
	public void build() {
		if (type != QueryType.SELECT || columns.isEmpty())
			return;
		
		query.append(String.format("\nORDER BY %s%s",
		                           columns.stream().map(ColumnInterface::sqlName).collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())),
		                           order != null ? " " + order : ""));
	}
	
}