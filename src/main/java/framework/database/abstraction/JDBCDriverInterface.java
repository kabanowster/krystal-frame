package framework.database.abstraction;

@FunctionalInterface
public interface JDBCDriverInterface {
	
	String getConnectionStringBase();
	
}