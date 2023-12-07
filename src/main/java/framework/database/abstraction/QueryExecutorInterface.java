package framework.database.abstraction;

import framework.KrystalFramework;
import framework.logging.LoggingInterface;

import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
	Map<? extends ProviderInterface, Properties> getConnectionProperties();
	
	/**
	 * Connection string is <i>"jdbc:driver//server/database"</i>
	 */
	Map<? extends ProviderInterface, String> getConnectionStrings();
	
	/*
	 * Read
	 */
	
	default QueryResultInterface read(QueryInterface query) {
		return read(getDefaultProvider(), query);
	}
	
	default QueryResultInterface read(ProviderInterface provider, QueryInterface query) {
		provider = Optional.ofNullable(query.getProvider()).orElse(provider);
		log().trace("--> Loading from database: " + provider.toString());
		QueryResultInterface qr = null;
		try (Connection conn = connectToProvider(provider)) {
			log().trace("    Connected Successfully.");
			
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
	 * @See {@link #write(ProviderInterface, QueryInterface...)}.
	 */
	default Integer write(QueryInterface... query) {
		return write(getDefaultProvider(), query);
	}
	
	/**
	 * Uses given {@link ProviderInterface Provider} to execute writing with given {@link QueryInterface Queries} (if these queries don't specify particular provider).
	 */
	default Integer write(ProviderInterface provider, QueryInterface... query) {
		return Stream.of(query)
		             .collect(Collectors.groupingBy(q -> Optional.ofNullable(q.getProvider()).orElse(provider)))
		             .entrySet()
		             .stream()
		             .mapToInt(
				             e ->
				             {
					             log().trace("--> Writing to Database: " + e.getKey().toString());
					             try (Connection conn = connectToProvider(e.getKey())) {
						             log().trace("    Connected Successfully.");
						             
						             return e.getValue().stream().mapToInt(q -> execute(conn, q)).sum();
						             
					             } catch (SQLException ex) {
						             log().fatal("!!! FATAL error during Database connection.");
						             ex.printStackTrace();
						             return 0;
					             }
				             }
		             )
		             .sum();
		
	}
	
	private int execute(Connection conn, QueryInterface query) {
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
		return DriverManager.getConnection(
				getConnectionStrings().get(provider),
				getConnectionProperties().get(provider)
		);
	}
	
	/**
	 * Mandatory properties to be set within <i><b>"provider_name.properties"</b></i>. Unlike other properties, their name may differ from common, driver-specific namings.
	 */
	enum MandatoryProperties {
		server, database
	}
	
}