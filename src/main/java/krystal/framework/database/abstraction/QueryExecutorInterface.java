package krystal.framework.database.abstraction;

import krystal.Tools;
import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingInterface;
import lombok.val;

import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Doing the dirty job of connecting through JDBC and processing the ResultSet into maintainable {@link QueryResultInterface}.
 */
public interface QueryExecutorInterface extends LoggingInterface {
	
	static QueryExecutorInterface getInstance() {
		return KrystalFramework.getSpringContext().getBean(QueryExecutorInterface.class);
	}
	
	QueryResultInterface process(ResultSet rs);
	
	ProviderInterface getDefaultProvider();
	
	/**
	 * Set of properties applied to the connection, including user and password, loaded from <i>provider.properties</i>.
	 */
	Map<ProviderInterface, Properties> getConnectionProperties();
	
	/**
	 * Connection string is <i>"jdbc:driver//server/database"</i>
	 */
	Map<ProviderInterface, String> getConnectionStrings();
	
	/*
	 * Initialization
	 */
	
	default void loadProviderProperties(ProviderInterface... providers) {
		val connectionProperties = getConnectionProperties();
		val connectionStrings = getConnectionStrings();
		
		Stream.of(providers).forEach(provider -> {
			val props = new Properties();
			try {
				props.load(Tools.getResource(KrystalFramework.getProvidersPropertiesDir(), provider.toString() + ".properties").openStream());
				connectionProperties.put(provider, props);
				
				// TODO throw if mandatory missing?
				connectionStrings.put(provider, provider.jdbcDriver().getConnectionStringBase() + props
						.entrySet()
						.stream()
						.filter(e -> Stream.of(MandatoryProperties.values()).map(Enum::toString).anyMatch(m -> m.equals(e.getKey().toString())))
						.sorted((e1, e2) -> e2.getKey().toString().compareTo(e1.getKey().toString()))
						.map(e -> e.getValue().toString())
						.collect(Collectors.joining("/")));
				
			} catch (IOException | IllegalArgumentException ex) {
				log().fatal(String.format("!!! Exception while loading '%s' provider properties file. Skipping.", provider));
			}
		});
	}
	
	/*
	 * Read
	 */
	
	default QueryResultInterface read(Query query) {
		return read(getDefaultProvider(), query);
	}
	
	default QueryResultInterface read(ProviderInterface provider, Query query) {
		var finalProvider = new AtomicReference<>(provider);
		Optional.ofNullable(query.getProvider()).ifPresent(finalProvider::set);
		
		log().trace("--> Loading from database: " + finalProvider);
		QueryResultInterface qr = null;
		try (Connection conn = connectToProvider(finalProvider.get())) {
			log().trace("    Connected Successfully.");
			query.setProvidersPacked(finalProvider.get());
			
			Statement sql = conn.createStatement();
			log().trace("    Query: " + query.sqlQuery());
			
			qr = process(sql.executeQuery(query.sqlQuery()));
			
		} catch (SQLException ex) {
			log().fatal("!!! FATAL error during Database connection.");
			ex.printStackTrace();
		}
		return qr;
	}
	
	/*
	 * Write
	 */
	
	/**
	 * Uses default provider.
	 *
	 * @See {@link #write(ProviderInterface, Query...)}.
	 */
	default Integer write(Query... query) {
		return write(getDefaultProvider(), query);
	}
	
	/**
	 * Uses given {@link ProviderInterface Provider} to execute writing with given {@link Query Queries} (if these queries don't specify particular provider).
	 */
	default Integer write(ProviderInterface provider, Query... query) {
		return Stream.of(query).collect(Collectors.groupingBy(q -> Optional.ofNullable(q.getProvider()).orElse(provider))).entrySet().stream().mapToInt(e -> {
			val finalProvider = e.getKey();
			log().trace("--> Writing to Database: " + finalProvider.toString());
			try (Connection conn = connectToProvider(finalProvider)) {
				log().trace("    Connected Successfully.");
				
				return e.getValue().stream().mapToInt(q -> {
					q.setProvidersPacked(finalProvider);
					return execute(conn, q);
				}).sum();
				
			} catch (SQLException ex) {
				log().fatal("!!! FATAL error during Database connection.");
				ex.printStackTrace();
				return 0;
			}
		}).sum();
		
	}
	
	private int execute(Connection conn, Query query) {
		try {
			PreparedStatement sql = conn.prepareStatement(query.sqlQuery());
			log().trace("  > Query: " + query.sqlQuery());
			int result = sql.executeUpdate();
			log().trace("    Executed rows: " + result);
			return result;
		} catch (SQLException ex) {
			log().fatal("!!! Failed query execution.");
			ex.printStackTrace();
			return 0;
		}
	}
	
	private Connection connectToProvider(ProviderInterface provider) throws SQLException {
		return DriverManager.getConnection(getConnectionStrings().get(provider), getConnectionProperties().get(provider));
	}
	
	/**
	 * Mandatory properties to be set within <i><b>"provider_name.properties"</b></i>. Unlike other properties, their name may differ from common, driver-specific namings.
	 */
	enum MandatoryProperties {
		server, database
	}
	
}