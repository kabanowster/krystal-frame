package krystal.framework.database.abstraction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import krystal.framework.database.persistence.PersistenceInterface;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rows and columns loaded from database.
 */
public interface QueryResultInterface {
	
	static <T> QueryResultInterface singleton(ColumnInterface column, T value) {
		return new QueryResultInterface() {
			@Override
			public List<Map<ColumnInterface, Object>> rows() {
				return ImmutableList.of(ImmutableMap.of(column, value));
			}
			
			@Override
			public Map<ColumnInterface, Class<?>> columns() {
				return ImmutableMap.of(column, value.getClass());
			}
		};
	}
	
	List<Map<ColumnInterface, Object>> rows();
	
	Map<ColumnInterface, Class<?>> columns();
	
	/**
	 * Overload method for {@link PersistenceInterface#mapQueryResult(QueryResultInterface, Class) PersistenceInterface.mapQueryResult()}. Easily transform rows and columns to objects.
	 *
	 * @see PersistenceInterface
	 */
	default <T> Stream<T> toStreamOf(Class<T> clazz) {
		return PersistenceInterface.mapQueryResult(this, clazz);
	}
	
	/**
	 * @return First row as Map.
	 */
	default Optional<Map<ColumnInterface, Object>> getRow() {
		return rows().stream().findFirst();
	}
	
	/**
	 * @param column
	 * 		- Column name.
	 * @return Values of a single column as list.
	 */
	default List<Object> getColumn(ColumnInterface column) {
		return rows().stream().map(row -> row.get(column)).collect(Collectors.toList());
	}
	
	/**
	 * @param columns
	 * 		- Columns names.
	 * @return Values of columns specified in argument as list of maps.
	 */
	default List<Map<ColumnInterface, Object>> getColumns(ColumnInterface... columns) {
		List<Map<ColumnInterface, Object>> buffer = new LinkedList<>();
		rows().forEach(row -> {
			Map<ColumnInterface, Object> vals = new HashMap<>();
			for (ColumnInterface column : columns) vals.put(column, row.get(column));
			buffer.add(vals);
		});
		return buffer;
	}
	
	/**
	 * @return First result (row 0, column 0) as Optional.
	 */
	default Optional<Object> getResult() {
		return rows().stream().findFirst().flatMap(r -> r.values().stream().findFirst());
	}
	
	/**
	 * @param column
	 * 		-Column name
	 * @return First row result at given column name.
	 */
	default Optional<Object> getResult(ColumnInterface column) {
		return rows().stream().findFirst().map(o -> o.get(column));
	}
	
}