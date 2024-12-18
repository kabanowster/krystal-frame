package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

import java.util.List;

interface OrderByInterface {
	
	Query pack();
	
	default OrderByKeyword orderBy(OrderByDirection order, ColumnInterface... columns) {
		return new OrderByKeyword(pack(), order, columns);
	}
	
	default OrderByKeyword orderBy(ColumnInterface... columns) {
		return new OrderByKeyword(pack(), OrderByDirection.ASC, columns);
	}
	
	default OrderByKeyword orderBy(List<OrderByDeclaration> declarations) {
		return new OrderByKeyword(pack(), declarations);
	}
	
}