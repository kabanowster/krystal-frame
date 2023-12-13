package framework.database.queryfactory;

import framework.database.abstraction.ColumnInterface;

interface OrderByInterface {
	
	Query pack();
	
	default OrderByKeyword orderBy(OrderByDirection order, ColumnInterface... columns) {
		return new OrderByKeyword(pack(), order, columns);
	}
	
	default OrderByKeyword orderBy(ColumnInterface... columns) {
		return new OrderByKeyword(pack(), columns);
	}
	
}