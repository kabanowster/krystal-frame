package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;

public interface GroupByInterface {
	
	Query pack();
	
	default GroupByStatement groupBy(ColumnInterface... columns) {
		return new GroupByStatement(pack(), columns);
	}
	
}