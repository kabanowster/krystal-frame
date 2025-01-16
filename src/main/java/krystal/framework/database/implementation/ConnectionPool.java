package krystal.framework.database.implementation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.Nullable;
import krystal.framework.database.abstraction.ConnectionPoolInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Hikari Connection Pool wrapper implementation.
 *
 * @see #configMap
 * @see #defaultConfig
 * @see #createConfig(Consumer)
 */
@Service
public class ConnectionPool implements ConnectionPoolInterface, LoggingInterface {
	
	private @Getter final Map<ProviderInterface, HikariDataSource> pools;
	private final QueryExecutorInterface queryExecutor;
	
	/**
	 * Map of Hikari configurations for providers.
	 */
	private final Map<ProviderInterface, HikariConfig> configMap;
	
	/**
	 * Map of configurators run upon initialisation, for each {@link ProviderInterface}, extending or overwriting {@link #defaultConfig}. The result {@link HikariConfig} can be accessed with {@link #getConfig(ProviderInterface)} method, after
	 * initialisation.
	 */
	private static final @Getter Map<ProviderInterface, Consumer<HikariConfig>> configurators = new HashMap<>();
	/**
	 * Default config is set before {@link #configurators} for each {@link ProviderInterface}.
	 */
	private static @Getter @Setter HikariConfig defaultConfig = createConfig(c -> {
		c.setMaximumPoolSize(3);
	});
	
	/**
	 * Set in millis the sleep interval while invoking {@link #getJDBCConnection(ProviderInterface)}. This supports the Virtual Threads unblocked behaviour, by producing the {@link Connection} with {@link CompletableFuture} rather than VT itself. The VT
	 * waits for CF result by sleeping (releasing) in provided intervals.
	 * If set to {@code 0}, the waiting is disabled and the carrying thread joins the CF immediately.
	 * <p>
	 * {@code Default: 100}.
	 */
	private static @Setter int sleepingInterval = 100;
	
	public ConnectionPool(QueryExecutorInterface queryExecutor) {
		this.queryExecutor = queryExecutor;
		this.configMap = new HashMap<>();
		log().debug("*** Building Connection Pool.");
		pools = Collections.synchronizedMap(new HashMap<>());
		
		queryExecutor.getConnectionStrings().keySet()
		             .forEach(this::createPool);
	}
	
	/**
	 * @see #sleepingInterval
	 */
	@Override
	public Connection getJDBCConnection(ProviderInterface provider) throws SQLException {
		
		AtomicReference<SQLException> exception = new AtomicReference<>();
		
		/*
		 * Support for VirtualPromise
		 * Blocking operation under VirtualThread, blocks the carrying thread, unless sleep() is being used.
		 * Blocking VT this way can lead to unpredictable deadlocking results.
		 */
		
		// Start a physical thread here to get the result.
		val connection = CompletableFuture.supplyAsync(() -> {
			try {
				return Optional.ofNullable(pools.get(provider))
				               .orElseGet(() -> createPool(provider))
				               .getConnection();
			} catch (SQLException e) {
				exception.set(e);
				return null;
			}
		});
		
		// VT will sleep-release here until the physical thread commits the result.
		if (sleepingInterval > 0) {
			while (!connection.isDone()) {
				try {
					Thread.sleep(sleepingInterval);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			
			if (exception.get() != null) throw exception.get();
		}
		
		return connection.join();
	}
	
	public HikariDataSource createPool(ProviderInterface provider) {
		log().trace("    Creating new pool for {}...", provider.name());
		
		val config = new HikariConfig();
		
		// defaultConfig.copyStateTo(config);
		// custom copy
		for (var field : HikariConfig.class.getDeclaredFields()) {
			if (!Modifier.isFinal(field.getModifiers()) && field.trySetAccessible()) {
				try {
					val value = field.get(defaultConfig);
					if (field.getType() == Properties.class) {
						field.set(config, new Properties((Properties) value));
					} else {
						field.set(config, value);
					}
				} catch (Exception e) {
					throw new RuntimeException("Failed to copy HikariConfig state: " + e.getMessage(), e);
				}
			}
		}
		
		config.setPoolName(provider.name() + " connection pool");
		config.setJdbcUrl(queryExecutor.getConnectionStrings().get(provider));
		queryExecutor.getConnectionProperties().get(provider).forEach((p, v) -> config.addDataSourceProperty(p.toString(), v));
		Optional.ofNullable(configurators.get(provider)).ifPresent(configurator -> configurator.accept(config));
		
		val dataSource = new HikariDataSource(config);
		pools.put(provider, dataSource);
		configMap.put(provider, config);
		dataSource.validate();
		return dataSource;
	}
	
	public static Map<ProviderInterface, HikariDataSource> pools() {
		return ((ConnectionPool) ConnectionPoolInterface.getInstance().orElseThrow()).getPools();
	}
	
	public static HikariConfig createConfig(Consumer<HikariConfig> configurator) {
		val config = new HikariConfig();
		configurator.accept(config);
		return config;
	}
	
	public @Nullable HikariConfig getConfig(ProviderInterface provider) {
		return pools.get(provider);
	}
	
}