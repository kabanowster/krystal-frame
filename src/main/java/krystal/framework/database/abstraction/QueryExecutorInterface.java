package krystal.framework.database.abstraction;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.database.implementation.ExecutionType;
import krystal.framework.database.implementation.QueryResult;
import krystal.framework.database.queryfactory.QueryType;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Doing the dirty job of connecting through database drivers and processing the ResultSet into maintainable {@link QueryResultInterface}.
 *
 * @see #loadProviders(List)
 * @see #execute(List)
 * @see ConnectionPoolInterface
 */
public interface QueryExecutorInterface extends LoggingInterface {
	
	static Optional<QueryExecutorInterface> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(QueryExecutorInterface.class));
		} catch (NullPointerException e) {
			return Optional.empty();
		} catch (NoSuchBeanDefinitionException e) {
			LoggingWrapper.ROOT_LOGGER.fatal(e.getMessage());
			return Optional.empty();
		}
	}
	
	/**
	 * Set of properties applied to the connection, including user and password, loaded from <i>provider.properties</i>.
	 */
	Map<ProviderInterface, Properties> getConnectionProperties();
	
	/**
	 * Connection string is <i>"jdbc:driver://"</i>
	 */
	Map<ProviderInterface, String> getConnectionStrings();
	
	/*
	 * Initialization
	 */
	
	/**
	 * Initialize connections properties for listed database DefaultProviders from {@code provider_name.properties} files within {@link KrystalFramework#getProvidersPropertiesDir()} providersPropertiesDir} directory.
	 */
	default void loadProviders(List<ProviderInterface> providers) {
		log().debug("*** Loading Provider Properties.");
		val connectionProperties = getConnectionProperties();
		val connectionStrings = getConnectionStrings();
		providers.forEach(provider -> {
			val props = new Properties();
			Tools.getResource(KrystalFramework.getProvidersPropertiesDir(), provider.name() + ".properties").ifPresent(
					path -> {
						log().trace("    Path: " + path.getPath());
						try {
							props.load(path.openStream());
							connectionProperties.put(provider, props);
							
							val mandatories = Stream.of(MandatoryProperties.values())
							                        .map(Enum::name)
							                        .map(props::get)
							                        .filter(Objects::nonNull)
							                        .map(Objects::toString)
							                        .toList();
							
							if (mandatories.size() != MandatoryProperties.values().length)
								throw new IllegalArgumentException("Mandatory provider properties not found.");
							
							connectionStrings.put(provider, provider.getDriver().getConnectionStringBase() + String.join("/", mandatories));
							
						} catch (IOException | IllegalArgumentException ex) {
							log().fatal(String.format("!!! Exception while loading '%s' provider properties file at '%s'. Skipping. %s", provider.name(), path.getPath(), ex.getMessage()));
						}
					}
			);
		});
	}
	
	/*
	 * Execute
	 */
	
	/**
	 * Execute the list of {@link Query queries}, read or write determined automatically
	 */
	default Stream<QueryResultInterface> execute(List<Query> queries) {
		
		return queries.stream()
		              .collect(Collectors.groupingBy(q -> Optional.ofNullable(q.getProvider()).orElse(KrystalFramework.getDefaultProvider())))
		              .entrySet()
		              .stream()
		              .flatMap(e -> {
			              val provider = e.getKey();
			              val driver = provider.getDriver();
			              
			              log().trace("--> Querying database: " + provider.name());
			              
			              return e.getValue()
			                      .stream()
			                      .collect(Collectors.groupingBy(q -> {
				                      q.setProvidersPacked(provider);
				                      val type = q.determineType();
				                      
				                      if (type == QueryType.SELECT) {
					                      return ExecutionType.read;
				                      } else {
					                      if (driver.getSupportedOutputtingStatements().contains(type))
						                      return ExecutionType.read;
					                      else return ExecutionType.write;
				                      }
			                      }))
			                      .entrySet()
			                      .stream()
			                      .flatMap(g -> executeJDBC(g.getKey(), provider, g.getValue()));
		              });
	}
	
	
	/*
	 * JDBC
	 */
	
	private Stream<QueryResultInterface> executeJDBC(ExecutionType exeType, ProviderInterface provider, List<Query> queries) {
		return switch (exeType) {
			case read -> readJDBC(provider, queries);
			case write -> writeJDBC(provider, queries);
		};
	}
	
	private Stream<QueryResultInterface> readJDBC(ProviderInterface provider, List<Query> queries) {
		return queries.stream().map(q -> {
			try (Connection conn = connectToJDBCProvider(provider)) {
				log().trace("  - Connected Successfully.");
				
				val sql = q.sqlQuery();
				log().trace("    Loader: " + sql);
				try {
					return (QueryResultInterface) new QueryResult(conn.createStatement().executeQuery(sql));
				} catch (SQLException e) {
					log().fatal("!!! Failed query execution.\n", e);
					return null;
				}
			} catch (SQLException e) {
				log().fatal("!!! FATAL error during Database connection.\n", e);
				return null;
			}
		}).filter(Objects::nonNull);
		
	}
	
	private Stream<QueryResultInterface> writeJDBC(ProviderInterface provider, List<Query> queries) {
		try (Connection conn = connectToJDBCProvider(provider)) {
			log().trace("    Connected Successfully.");
			val batch = conn.createStatement();
			
			queries.forEach(q -> {
				val sql = q.sqlQuery();
				log().trace("    Loader: " + sql);
				try {
					batch.addBatch(sql);
				} catch (SQLException e) {
					log().fatal("!!! Failed adding query to the batch.\n", e);
				}
			});
			
			val result = batch.executeBatch();
			
			return Arrays.stream(result).mapToObj(i -> QueryResult.of(QueryResultInterface.singleton(ColumnInterface.of("#"), i)));
			
		} catch (SQLException e) {
			log().fatal("!!! FATAL error during Database connection.\n", e);
			return Stream.empty();
		}
	}
	
	/*
	 * Connectors
	 */
	
	private Connection connectToJDBCProvider(ProviderInterface provider) throws SQLException {
		try {
			return ConnectionPoolInterface.getInstance()
			                              .map(c -> {
				                              try {
					                              return c.getJDBCConnection(provider);
				                              } catch (SQLException e) {
					                              throw new RuntimeException(e);
				                              }
			                              })
			                              .orElse(DriverManager.getConnection(getConnectionStrings().get(provider), getConnectionProperties().get(provider)));
		} catch (RuntimeException e) {
			throw new SQLException(e);
		}
	}
	
	/**
	 * Mandatory properties to be set within <i><b>"provider_name.properties"</b></i>. Unlike other properties, their name may differ from common, driver-specific namings.
	 */
	enum MandatoryProperties {
		host, database
	}
	
}