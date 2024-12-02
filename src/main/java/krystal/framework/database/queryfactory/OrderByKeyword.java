package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderByKeyword extends Query {
	
	private final List<OrderByDeclaration> order = Collections.synchronizedList(new LinkedList<>());
	
	public OrderByKeyword(Query query, OrderByDirection order, ColumnInterface... columns) {
		super(query);
		for (var column : columns) this.order.add(new OrderByDeclaration(order, column));
	}
	
	public OrderByKeyword(Query query, List<OrderByDeclaration> order) {
		super(query);
		this.order.addAll(order);
	}
	
	public OrderByKeyword orderBy(OrderByDirection direction, ColumnInterface column) {
		order.add(new OrderByDeclaration(direction, column));
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (type != QueryType.SELECT || order.isEmpty()) throw new IllegalArgumentException();
		
		query.append(" ORDER BY ");
		query.append(order.stream()
		                  .map(o -> String.format("%s %s", o.column().getSqlName(), o.order()))
		                  .collect(Collectors.joining(", ")));
	}
	
}