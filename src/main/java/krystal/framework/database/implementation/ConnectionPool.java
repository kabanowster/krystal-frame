package krystal.framework.database.implementation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import krystal.framework.database.abstraction.ConnectionPoolInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hikari Connection Pool wrapper implementation.
 *
 * @see #getMaximumPoolSizes()
 * @see #getDefaultMaximumPoolSize()
 * @see #getDefaultConnectionKeepAlive()
 */
@Service
public class ConnectionPool implements ConnectionPoolInterface, LoggingInterface {
	
	private @Getter final Map<ProviderInterface, HikariDataSource> pools;
	private final QueryExecutorInterface queryExecutor;
	
	/**
	 * Set the maximum connection pool size for particular {@link ProviderInterface} by adding to this collection.
	 */
	private static @Getter final Map<ProviderInterface, Integer> maximumPoolSizes = new HashMap<>();
	
	/**
	 * Default value if not specified within {@link #getMaximumPoolSizes()}. Set or 3 by default.
	 */
	private static @Getter @Setter int defaultMaximumPoolSize = 3;
	
	/**
	 * Default keep-alive for a connection as defined in {@link HikariDataSource#getKeepaliveTime()} ()}.
	 */
	private static @Getter @Setter Duration defaultConnectionKeepAlive;
	
	ConnectionPool(QueryExecutorInterface queryExecutor) {
		this.queryExecutor = queryExecutor;
		log().debug("*** Building Connection Pool.");
		pools = Collections.synchronizedMap(new HashMap<>());
		
		queryExecutor.getConnectionStrings().keySet()
		             .forEach(this::createPool);
	}
	
	@SuppressWarnings("resource")
	@Override
	public Connection getJDBCConnection(ProviderInterface provider) throws SQLException {
		return Optional.ofNullable(pools.get(provider))
		               .orElseGet(() -> createPool(provider)).getConnection();
	}
	
	private HikariDataSource createPool(ProviderInterface provider) {
		log().trace("    Creating new pool for {}...", provider.name());
		
		val config = new HikariConfig();
		config.setJdbcUrl(queryExecutor.getConnectionStrings().get(provider));
		queryExecutor.getConnectionProperties().get(provider).forEach((p, v) -> config.addDataSourceProperty(p.toString(), v));
		
		config.setMaximumPoolSize(Optional.ofNullable(maximumPoolSizes.get(provider)).orElse(defaultMaximumPoolSize));
		config.setPoolName(provider.name() + " connection pool");
		Optional.ofNullable(defaultConnectionKeepAlive).ifPresent(duration -> config.setKeepaliveTime(duration.toMillis()));
		
		val dataSource = new HikariDataSource(config);
		pools.put(provider, dataSource);
		return dataSource;
	}
	
	public static Map<ProviderInterface, HikariDataSource> pools() {
		return ((ConnectionPool) ConnectionPoolInterface.getInstance().orElseThrow()).getPools();
	}
	
}