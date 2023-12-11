package framework.database.implementation;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;
import framework.database.abstraction.QueryResultInterface;
import framework.logging.LoggingInterface;
import krystal.Tools;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
				var colName = metaData.getColumnName(i);
				var type = sqlTypetoClass(metaData.getColumnType(i));
				columns.put(() -> colName, type);
			}
			
			// Data
			while (rs.next()) {
				Map<ColumnInterface, Object> valuesMap = new LinkedHashMap<>();
				for (Map.Entry<ColumnInterface, Class<?>> entry : columns.entrySet())
					valuesMap.put(entry.getKey(), entry.getValue().cast(rs.getObject(entry.getKey().sqlName())));
				rows.add(valuesMap);
			}
			
			log().trace("    Query loaded.");
		} catch (SQLException ex) {
			log().fatal("!!! Error processing the ResultSet.");
			ex.printStackTrace();
		}
		
	}
	
	/**
	 * Translates a data type from an integer (java.sql.Types value) to a string that represents the corresponding class.
	 */
	public static Class<?> sqlTypetoClass(int type) {
		return switch (type) {
			case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> String.class;
			case Types.NUMERIC, Types.DECIMAL -> java.math.BigDecimal.class;
			case Types.BIT -> Boolean.class;
			case Types.TINYINT -> Byte.class;
			case Types.SMALLINT -> Short.class;
			case Types.INTEGER -> Integer.class;
			case Types.BIGINT -> Long.class;
			case Types.REAL, Types.FLOAT -> Float.class;
			case Types.DOUBLE -> Double.class;
			case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Byte[].class;
			case Types.DATE -> java.sql.Date.class;
			case Types.TIME -> java.sql.Time.class;
			case Types.TIMESTAMP -> java.sql.Timestamp.class;
			default -> Object.class;
		};
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(System.lineSeparator());
		result.append(columns.entrySet().stream()
		                     .map(e -> String.format("%s (%s)", e.getKey().sqlName(), e.getValue().getSimpleName()))
		                     .collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())))
		      .append(System.lineSeparator());
		rows.forEach(row -> result.append(Tools.concat(KrystalFramework.getDefaultDelimeter(), row.values().stream())).append(System.lineSeparator()));
		return result.toString();
	}
	
}