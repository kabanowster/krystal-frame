package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;
import lombok.experimental.UtilityClass;

/**
 * Quick factory for basic {@link Query} abstractions.
 */
@UtilityClass
public class Q {
	
	public TableInterface t(String name) {
		return () -> name;
	}
	
	public ColumnInterface c(String name) {
		return () -> name;
	}
	
	public Query q(String sql) {
		return Query.of(sql);
	}
	
}