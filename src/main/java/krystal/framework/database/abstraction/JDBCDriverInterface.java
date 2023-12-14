package krystal.framework.database.abstraction;

@FunctionalInterface
public interface JDBCDriverInterface {
	
	String getConnectionStringBase();
	
	default ProviderInterface asProvider() {
		return () -> this;
	}
	
}