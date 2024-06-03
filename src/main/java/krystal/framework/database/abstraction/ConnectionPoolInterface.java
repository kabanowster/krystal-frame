package krystal.framework.database.abstraction;

import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

/**
 * If implemented, {@link QueryExecutorInterface} will use it to load connections. If not, regular {@link java.sql.DriverManager#getConnection(String, Properties) DriverManager#getConnection(String, Properties)} will be invoked <b>for each</b> query call.
 */
@FunctionalInterface
public interface ConnectionPoolInterface {
	
	Connection getJDBCConnection(ProviderInterface provider) throws SQLException;
	
	static Optional<ConnectionPoolInterface> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(ConnectionPoolInterface.class));
		} catch (NullPointerException e) {
			return Optional.empty();
		} catch (NoSuchBeanDefinitionException e) {
			LoggingInterface.logger().warn(e.getMessage());
			return Optional.empty();
		}
	}
	
}