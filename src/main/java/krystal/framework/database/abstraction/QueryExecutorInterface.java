package krystal.framework.database.abstraction;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import io.r2dbc.spi.Result;
import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.database.implementation.ExecutionType;
import krystal.framework.database.implementation.QueryResult;
import krystal.framework.database.implementation.QueryResultRow;
import krystal.framework.logging.LoggingInterface;
import lombok.val;
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
	
	static QueryExecutorInterface getInstance() {
		return KrystalFramework.getSpringContext().getBean(QueryExecutorInterface.class);
	}
	
	ProviderInterface getDefaultProvider();
	
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
	
	default void loadProviderProperties(ProviderInterface... providers) {
		log().debug("*** Loading Provider Properties.");
		val connectionProperties = getConnectionProperties();
		val connectionStrings = getConnectionStrings();
		
		Stream.of(providers).forEach(provider -> {
			val props = new Properties();
			val path = Tools.getResource(KrystalFramework.getProvidersPropertiesDir(), provider.toString() + ".properties");
			log().trace("    Path: " + path.getPath());
			try {
				props.load(path.openStream());
				connectionProperties.put(provider, props);
				
				// TODO throw if mandatory missing?
				connectionStrings.put(provider, provider.dbcDriver().getConnectionStringBase() + props
						.entrySet()
						.stream()
						.filter(e -> Stream.of(MandatoryProperties.values()).map(Enum::toString).anyMatch(m -> m.equals(e.getKey().toString())))
						.sorted((e1, e2) -> e2.getKey().toString().compareTo(e1.getKey().toString()))
						.map(e -> e.getValue().toString())
						.collect(Collectors.joining("/")));
				
			} catch (IOException | IllegalArgumentException ex) {
				log().fatal(String.format("!!! Exception while loading '%s' provider properties file at '%s'. Skipping.", provider, path.getPath()));
			}
		});
	}
	
	/*
	 * Execute
	 */
	
	default Flux<QueryResultInterface> execute(List<Query> queries) {
		return Flux.fromStream(queries.stream().collect(Collectors.groupingBy(q -> Optional.ofNullable(q.getProvider()).orElse(getDefaultProvider()))).entrySet().stream())
		           .concatMap(e -> {
			           val provider = e.getKey();
			           val driver = provider.dbcDriver();
			           
			           log().trace("--> Querying database: " + provider);
			           
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
				                      case jdbc -> executeJDBC(g.getKey(), provider, g.getValue());
				                      case r2dbc -> executeR2DBC(g.getKey(), provider, g.getValue());
			                      });
		           });
	}
	
	/*
	 * JDBC
	 */
	
	private Flux<QueryResultInterface> executeJDBC(ExecutionType exeType, ProviderInterface provider, List<Query> queries) {
		return switch (exeType) {
			case read -> readJDBC(provider, queries);
			case write -> writeJDBC(provider, queries);
		};
	}
	
	private Flux<QueryResultInterface> readJDBC(ProviderInterface provider, List<Query> queries) {
		return Flux.fromStream(queries.stream().map(q -> {
			try (Connection conn = connectToJDBCProvider(provider)) {
				log().trace("  - Connected Successfully.");
				
				val sql = q.sqlQuery();
				log().trace("    Query: " + sql);
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
		}));
		
	}
	
	private Flux<QueryResultInterface> writeJDBC(ProviderInterface provider, List<Query> queries) {
		try (Connection conn = connectToJDBCProvider(provider)) {
			log().trace("    Connected Successfully.");
			val batch = conn.createStatement();
			
			queries.forEach(q -> {
				val sql = q.sqlQuery();
				log().trace("    Query: " + sql);
				try {
					batch.addBatch(sql);
				} catch (SQLException e) {
					log().fatal("!!! Failed adding query to the batch.\n" + e.getMessage());
				}
			});
			
			val result = batch.executeBatch();
			
			return Flux.fromStream(Arrays.stream(result).mapToObj(i -> QueryResult.of(QueryResultInterface.singleton(ColumnInterface.of("#"), i))));
			
		} catch (SQLException e) {
			log().fatal("!!! FATAL error during Database connection.\n" + e.getMessage());
			return Flux.empty();
		}
	}
	
	/*
	 * R2DBC
	 */
	
	private Flux<QueryResultInterface> executeR2DBC(ExecutionType exeType, ProviderInterface provider, List<Query> queries) {
		val execution = connectToR2DBCProvider(provider)
				.flatMapMany(c -> {
					log().trace("    Connected Successfully.");
					val batch = c.createBatch();
					
					queries.forEach(q -> {
						q.setProvidersPacked(provider);
						val sql = q.sqlQuery();
						log().trace("  > Query: " + sql);
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
		
		return DriverManager.getConnection(getConnectionStrings().get(provider), getConnectionProperties().get(provider));
	}
	
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