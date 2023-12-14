package framework.database.abstraction;

public interface Q {
	
	static TableInterface t(String name) {
		return () -> name;
	}
	
	static ColumnInterface c(String name) {
		return () -> name;
	}
	
}