package krystal.framework.database.abstraction;

public interface Q {
	
	static TableInterface t(String name) {
		return () -> name;
	}
	
	static ColumnInterface c(String name) {
		return () -> name;
	}
	
	static Query q(String sql) {
		return Query.of(sql);
	}
	
}