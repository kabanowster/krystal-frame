package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderByKeyword extends Query {
	
	private final Map<OrderByDirection, Set<ColumnInterface>> order = Collections.synchronizedMap(new LinkedHashMap<>());
	
	public OrderByKeyword(Query query, OrderByDirection order, ColumnInterface... columns) {
		super(query);
		this.order.put(order, Stream.of(columns).collect(Collectors.toSet()));
	}
	
	public OrderByKeyword(Query query, Map<OrderByDirection, Set<ColumnInterface>> order) {
		super(query);
		this.order.putAll(order);
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (type != QueryType.SELECT || order.isEmpty()) throw new IllegalArgumentException();
		
		query.append(" ORDER BY ");
		query.append(order.entrySet()
		                  .stream()
		                  .flatMap(e -> e.getValue()
		                                 .stream()
		                                 .map(c -> String.format("%s %s", c.getSqlName(), e.getKey())))
		                  .collect(Collectors.joining(", ")));
	}
	
}