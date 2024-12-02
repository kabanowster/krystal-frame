package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;

public record OrderByDeclaration(OrderByDirection order, ColumnInterface column) {
	
	public static OrderByDeclaration asc(ColumnInterface column) {
		return new OrderByDeclaration(OrderByDirection.ASC, column);
	}
	
	public static OrderByDeclaration desc(ColumnInterface column) {
		return new OrderByDeclaration(OrderByDirection.DESC, column);
	}
	
}