package krystal.framework.database.abstraction;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import io.r2dbc.spi.Result;
import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.database.implementation.*;
import krystal.framework.logging.LoggingInterface;
import krystal.framework.logging.LoggingWrapper;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Doing the dirty job of connecting through database drivers and processing the ResultSet into maintainable {@link QueryResultInterface}.
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
							
							connectionStrings.put(provider, provider.dbcDriver().getConnectionStringBase() + String.join("/", mandatories));
							
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
	 * The FLux reactive functionalities are superseded by Virtual Threads.
	 */
	@Deprecated(forRemoval = true)
	default Flux<QueryResultInterface> executeFlux(List<Query> queries) {
		return Flux.fromStream(queries.stream().collect(Collectors.groupingBy(q -> Optional.ofNullable(q.getProvider()).orElse(KrystalFramework.getDefaultProvider()))).entrySet().stream())
		           .concatMap(e -> {
			           val provider = e.getKey();
			           val driver = provider.dbcDriver();
			           
			           log().trace("--> Querying database: " + provider.name());
			           
			           return Flux.fromStream(
					                      e.getValue().stream().collect(Collectors.groupingBy(q -> {
						                      q.setProvidersPacked(provider);
						                      val type = q.determineType();
						                      
						                      return switch (type) {
							                      case SELECT -> ExecutionType.read;
							                      case INSERT -> {
								                      // drivers which return inserted rows as result
								                      if (List.of(
										                      DBCDrivers.jdbcAS400,
										                      DBCDrivers.jdbcSQLServer,
										                      DBCDrivers.r2dbcSQLServer
								                      ).contains(driver)) yield ExecutionType.read;
								                      else yield ExecutionType.write;
							                      }
							                      default -> ExecutionType.write;
						                      };
					                      })).entrySet().stream())
			                      .concatMap(g -> switch (driver.getDriverType()) {
				                      case jdbc -> Flux.fromStream(executeJDBC(g.getKey(), provider, g.getValue()));
				                      case r2dbc -> executeR2DBC(g.getKey(), provider, g.getValue());
			                      });
		           });
	}
	
	default Stream<QueryResultInterface> execute(List<Query> queries) {
		
		return queries.stream()
		              .collect(Collectors.groupingBy(q -> Optional.ofNullable(q.getProvider()).orElse(KrystalFramework.getDefaultProvider())))
		              .entrySet()
		              .stream()
		              .filter(e -> e.getKey().dbcDriver().getDriverType() == DriverType.jdbc)
		              .flatMap(e -> {
			              val provider = e.getKey();
			              val driver = provider.dbcDriver();
			              
			              log().trace("--> Querying database: " + provider.name());
			              
			              return e.getValue()
			                      .stream()
			                      .collect(Collectors.groupingBy(q -> {
				                      q.setProvidersPacked(provider);
				                      val type = q.determineType();
				                      
				                      return switch (type) {
					                      case SELECT -> ExecutionType.read;
					                      case INSERT -> {
						                      // drivers which return inserted rows as result
						                      if (List.of(
								                      DBCDrivers.jdbcAS400,
								                      DBCDrivers.jdbcSQLServer
						                      ).contains(driver)) yield ExecutionType.read;
						                      else yield ExecutionType.write;
					                      }
					                      default -> ExecutionType.write;
				                      };
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
					return new QueryResult(conn.createStatement().executeQuery(sql));
				} catch (SQLException e) {
					log().fatal("!!! Failed query execution.\n" + e.getMessage());
					return null;
				}
			} catch (SQLException e) {
				log().fatal("!!! FATAL error during Database connection.\n" + e.getMessage());
				return null;
			}
		});
		
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
					log().fatal("!!! Failed adding query to the batch.\n" + e.getMessage());
				}
			});
			
			val result = batch.executeBatch();
			
			return Arrays.stream(result).mapToObj(i -> QueryResult.of(QueryResultInterface.singleton(ColumnInterface.of("#"), i)));
			
		} catch (SQLException e) {
			log().fatal("!!! FATAL error during Database connection.\n" + e.getMessage());
			return Stream.empty();
		}
	}
	
	/*
	 * R2DBC
	 */
	@Deprecated(forRemoval = true)
	private Flux<QueryResultInterface> executeR2DBC(ExecutionType exeType, ProviderInterface provider, List<Query> queries) {
		val execution = connectToR2DBCProvider(provider)
				                .flatMapMany(c -> {
					                log().trace("    Connected Successfully.");
					                val batch = c.createBatch();
					                
					                queries.forEach(q -> {
						                q.setProvidersPacked(provider);
						                val sql = q.sqlQuery();
						                log().trace("  > Loader: " + sql);
						                batch.add(sql);
					                });
					                
					                return Flux.from(batch.execute())
					                           .doFinally(st -> c.close());
				                });
		
		return switch (exeType) {
			case read -> execution
					             .flatMapSequential(r -> r.map((row, metadata) -> new QueryResultRow(row, metadata, r.hashCode())))
					             .groupBy(queryResultRow -> queryResultRow.resultHash().get())
					             .flatMapSequential(r -> r.collectList().map(QueryResult::new));
			case write -> execution
					              .flatMapSequential(Result::getRowsUpdated)
					              .map(l -> QueryResult.of(QueryResultInterface.singleton(ColumnInterface.of("#"), l)));
		};
		
	}
	
	/*
	 * Connectors
	 */
	
	private Connection connectToJDBCProvider(ProviderInterface provider) throws SQLException {
		return ConnectionPoolInterface.getInstance()
		                              .map(c -> {
			                              try {
				                              return c.getJDBCConnection(provider);
			                              } catch (SQLException e) {
				                              throw logFatalAndThrow(e);
			                              }
		                              })
		                              .orElse(DriverManager.getConnection(getConnectionStrings().get(provider), getConnectionProperties().get(provider)));
	}
	
	@Deprecated(forRemoval = true)
	private Mono<? extends io.r2dbc.spi.Connection> connectToR2DBCProvider(ProviderInterface provider) {
		val options = ConnectionFactoryOptions.builder();
		options.option(Option.valueOf("driver"), provider.dbcDriver().getDriverName());
		getConnectionProperties().get(provider).forEach((key, value) -> options.option(Option.valueOf(key.toString()), value));
		
		return Mono.from(ConnectionFactories.get(options.build()).create());
	}
	
	/**
	 * Mandatory properties to be set within <i><b>"provider_name.properties"</b></i>. Unlike other properties, their name may differ from common, driver-specific namings.
	 */
	enum MandatoryProperties {
		host, database
	}
	
}