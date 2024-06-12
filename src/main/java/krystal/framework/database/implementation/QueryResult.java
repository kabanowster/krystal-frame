package krystal.framework.database.implementation;

import krystal.StringRenderer;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.val;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Parses and stores the ResultSet data.
 *
 * @author Wiktor Kabanow
 */
public record QueryResult(List<Map<ColumnInterface, Object>> rows, Map<ColumnInterface, Class<?>> columns) implements QueryResultInterface, LoggingInterface {
	
	public QueryResult() {
		this(Collections.synchronizedList(new ArrayList<>()), Collections.synchronizedMap(new LinkedHashMap<>()));
	}
	
	public QueryResult(ResultSet rs) {
		this();
		
		try {
			log().trace(" -> Loading QueryResult.");
			
			// Store columns information within QR
			ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				val colName = metaData.getColumnName(i);
				
				Class<?> type = Object.class;
				try {
					type = Class.forName(metaData.getColumnClassName(i));
				} catch (ClassNotFoundException ignored) {
				}
				columns.put(() -> colName, type);
			}
			
			// Data
			while (rs.next()) {
				Map<ColumnInterface, Object> row = Collections.synchronizedMap(LinkedHashMap.newLinkedHashMap(columns.size()));
				// classic for-loop to catch exception
				for (Map.Entry<ColumnInterface, Class<?>> entry : columns.entrySet()) {
					val value = rs.getObject(entry.getKey().getSqlName());
					Object cast;
					try {
						cast = entry.getValue().cast(value);
					} catch (ClassCastException ex) {
						cast = String.valueOf(value);
					}
					row.put(entry.getKey(), cast);
				}
				rows.add(row);
			}
			
			log().trace("    Loader loaded %s rows.".formatted(rows.size()));
		} catch (SQLException ex) {
			log().fatal("!!! Error processing the ResultSet.\n{}", ex.getMessage());
		}
		
	}
	
	public QueryResult(QueryResultInterface qr) {
		this();
		
		columns.putAll(qr.columns());
		rows.addAll(qr.rows());
	}
	
	public static QueryResultInterface of(QueryResultInterface qr) {
		return new QueryResult(qr);
	}
	
	@Override
	public String toString() {
		return StringRenderer.renderTable(
				columns.entrySet().stream()
				       .map(e -> String.format("%s (%s)", e.getKey().getSqlName(), e.getValue().getSimpleName()))
				       .toList(),
				rows.parallelStream()
				    .map(r -> r.values().stream().map(String::valueOf).toList())
				    .toList()
		);
	}
	
}