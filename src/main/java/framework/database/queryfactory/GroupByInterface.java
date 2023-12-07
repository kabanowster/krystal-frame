package framework.database.queryfactory;

import framework.database.abstraction.ColumnInterface;

public interface GroupByInterface {
	
	public abstract Query pack();
	
	default GroupByStatement groupBy(ColumnInterface... columns) {
		return new GroupByStatement(pack(), columns);
	}
	
}