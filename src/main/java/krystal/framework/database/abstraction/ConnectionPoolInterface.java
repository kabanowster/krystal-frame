package krystal.framework.database.abstraction;

import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingWrapper;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

@FunctionalInterface
public interface ConnectionPoolInterface {
	
	Connection getJDBCConnection(ProviderInterface provider) throws SQLException;
	
	static Optional<ConnectionPoolInterface> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(ConnectionPoolInterface.class));
		} catch (NullPointerException e) {
			return Optional.empty();
		} catch (NoSuchBeanDefinitionException e) {
			LoggingWrapper.ROOT_LOGGER.warn(e.getMessage());
			return Optional.empty();
		}
	}
	
}