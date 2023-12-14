package krystal.framework.database.implementation;

import krystal.CompletablePresent;
import krystal.StringRenderer;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.val;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Parses and stores the ResultSet data.
 *
 * @author Wiktor Kabanow
 */
public record QueryResult(List<Map<ColumnInterface, Object>> rows, Map<ColumnInterface, Class<?>> columns) implements QueryResultInterface, LoggingInterface {
	
	@SuppressWarnings("CallToPrintStackTrace")
	public QueryResult(ResultSet rs) {
		this(new LinkedList<>(), new LinkedHashMap<>());
		
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
				Map<ColumnInterface, Object> valuesMap = new LinkedHashMap<>();
				for (Map.Entry<ColumnInterface, Class<?>> entry : columns.entrySet())
					valuesMap.put(
							entry.getKey(),
							CompletablePresent
									.supply(rs.getObject(entry.getKey().sqlName()))
									.thenApply(o -> {
										try {
											return entry.getValue().cast(o);
										} catch (ClassCastException ex) {
											return String.valueOf(o);
										}
									}).getResult().orElse(null)
					);
				rows.add(valuesMap);
			}
			
			log().trace("    Query loaded %s rows.".formatted(rows.size()));
		} catch (SQLException ex) {
			log().fatal("!!! Error processing the ResultSet.");
			ex.printStackTrace();
		}
		
	}
	
	@Override
	public String toString() {
		return StringRenderer.renderTable(
				columns.entrySet().stream()
				       .map(e -> String.format("%s (%s)", e.getKey().sqlName(), e.getValue().getSimpleName()))
				       .toList(),
				rows.parallelStream()
				    .map(r -> r.values().stream().map(String::valueOf).toList())
				    .toList()
		);
	}
	
}