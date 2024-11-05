package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.Map;
import java.util.Set;

interface OrderByInterface {
	
	Query pack();
	
	default OrderByKeyword orderBy(OrderByDirection order, ColumnInterface... columns) {
		return new OrderByKeyword(pack(), order, columns);
	}
	
	default OrderByKeyword orderBy(ColumnInterface... columns) {
		return new OrderByKeyword(pack(), OrderByDirection.ASC, columns);
	}
	
	default OrderByKeyword orderBy(Map<OrderByDirection, Set<ColumnInterface>> order) {
		return new OrderByKeyword(pack(), order);
	}
	
}