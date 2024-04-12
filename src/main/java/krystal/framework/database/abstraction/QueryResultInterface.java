package krystal.framework.database.abstraction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import krystal.VirtualPromise;
import krystal.framework.database.persistence.PersistenceInterface;
import lombok.val;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Rows and columns loaded from database.
 *
 * @see PersistenceInterface
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
	
	static QueryResultInterface empty() {
		return new QueryResultInterface() {
			@Override
			public List<Map<ColumnInterface, Object>> rows() {
				return List.of();
			}
			
			@Override
			public Map<ColumnInterface, Class<?>> columns() {
				return Map.of();
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
	default <T> VirtualPromise<Stream<T>> toStreamOf(Class<T> clazz) {
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
	 * @return ValuesColumn of a single column as list.
	 */
	default List<Object> getColumn(ColumnInterface column) {
		val equalColumn = ColumnInterface.pickEqual(column, columns().keySet()).orElseThrow();
		return rows().stream().map(row -> row.get(equalColumn)).collect(Collectors.toList());
	}
	
	/**
	 * @param columns
	 * 		- Columns names.
	 * @return ValuesColumn of columns specified in argument as list of maps.
	 */
	default List<LinkedHashMap<ColumnInterface, Object>> getColumns(ColumnInterface... columns) {
		val columnsKeys = columns().keySet();
		val cols = Arrays.stream(columns)
		                 .map(c -> ColumnInterface.pickEqual(c, columnsKeys).orElseThrow())
		                 .toList();
		return rows().stream()
		             .map(row -> row.entrySet().stream()
		                            .filter(e -> cols.contains(e.getKey()))
		                            .collect(Collectors.toMap(
				                            Entry::getKey,
				                            Entry::getValue,
				                            (a, b) -> a,
				                            LinkedHashMap::new)))
		             .toList();
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
		val equalColumn = ColumnInterface.pickEqual(column, columns().keySet()).orElseThrow();
		return rows().stream().findFirst().map(o -> o.get(equalColumn));
	}
	
	/**
	 * Rebuild result - un-pivoting provided {@link ColumnInterface fieldsColumn} into {@link ColumnInterface columns} with corresponding values of result's {@link ColumnInterface valuesColumn}, as {@link String}. If  provided, the newly added columns
	 * will be built on and restricted to {@link ColumnInterface intoColumns}, even if they do not get corresponding values (would be {@code null}). If not provided {@link ColumnInterface intoColumns}, the new generated columns will be added in natural
	 * sorted order.
	 */
	default QueryResultInterface unpivot(ColumnInterface fieldsColumn, ColumnInterface valuesColumn, ColumnInterface... intoColumns) {
		
		// The interfaces have no equals, so we need to pull the corresponding ones from the object...
		val equalFieldsColumn = ColumnInterface.pickEqual(fieldsColumn, columns().keySet()).orElseThrow();
		val equalValuesColumn = ColumnInterface.pickEqual(valuesColumn, columns().keySet()).orElseThrow();
		
		// group columns that stay
		final Map<ColumnInterface, Class<?>> groupBy = new LinkedHashMap<>(columns());
		groupBy.remove(equalFieldsColumn);
		groupBy.remove(equalValuesColumn);
		
		// define new columns, either provided or derived from fieldsColumn
		final Map<ColumnInterface, Class<?>> newColumns =
				(
						intoColumns.length > 0 ?
						Arrays.stream(intoColumns) :
						rows().stream()
						      .map(row -> row.get(equalFieldsColumn))
						      .filter(Objects::nonNull)
						      .map(Object::toString)
						      .distinct()
						      .sorted()
						      .map(ColumnInterface::of)
				).collect(Collectors.toMap(
						c -> c,
						c -> String.class
						// (a, b) -> a,
						// LinkedHashMap::new
				));
		
		// initial grouping and un-pivot transformation
		List<Map<ColumnInterface, Object>> rowsResult = new LinkedList<>();
		val batchSize = 100000;
		val size = rows().size();
		for (int i = 0; i < size; i += batchSize) {
			rowsResult.addAll(rows().subList(i, Math.min(i + batchSize, size))
			                        .stream()
			                        .collect(Collectors.groupingBy(
					                        row -> row.entrySet().stream()
					                                  .filter(e -> groupBy.containsKey(e.getKey()))
					                                  .map(Entry::getValue)
					                                  .toArray(),
					                        Collector.of(
							                        () -> new LinkedHashMap<ColumnInterface, Object>(groupBy.size() + newColumns.size()),
							                        (group, r) -> {
								                        // un-pivot each row
								                        groupBy.forEach((c, t) -> group.put(c, r.get(c)));
								                        newColumns.forEach((c, t) -> group.put(c, Optional.ofNullable(r.get(equalValuesColumn)).map(Object::toString).orElse(null)));
							                        },
							                        (a, b) -> {
								                        // merge rows
								                        a.forEach(b::putIfAbsent);
								                        return b;
							                        }
					                        )))
			                        .values());
		}
		
		while (needsReducing(groupBy, rowsResult))
			rowsResult = reduceGroupedRows(groupBy, rowsResult, 1000);
		
		val finalResult = rowsResult;
		
		// merge columns containers
		groupBy.putAll(newColumns);
		
		return new QueryResultInterface() {
			@Override
			public List<Map<ColumnInterface, Object>> rows() {
				return finalResult; // new LinkedList<>(rowsResult);
			}
			
			@Override
			public Map<ColumnInterface, Class<?>> columns() {
				return groupBy; // new LinkedHashMap<>(groupBy);
			}
		};
		
	}
	
	private List<Map<ColumnInterface, Object>> reduceGroupedRows(Map<ColumnInterface, Class<?>> groupBy, List<Map<ColumnInterface, Object>> rows, int batchSize) {
		List<Map<ColumnInterface, Object>> result = new LinkedList<>();
		val size = rows.size();
		for (int i = 0; i < size; i += batchSize) {
			result.addAll(
					rows.subList(i, Math.min(i + batchSize, size)).stream()
					    .collect(Collectors.groupingBy(
							    row -> row.entrySet().stream()
							              .filter(e -> groupBy.containsKey(e.getKey()))
							              .map(Entry::getValue)
							              .toArray(),
							    Collector.of(
									    LinkedHashMap<ColumnInterface, Object>::new,
									    LinkedHashMap::putAll,
									    (a, b) -> {
										    a.forEach(b::putIfAbsent);
										    return b;
									    }
							    )))
					    .values());
		}
		return result;
	}
	
	private boolean needsReducing(Map<ColumnInterface, Class<?>> groupBy, List<Map<ColumnInterface, Object>> rows) {
		return rows.stream().map(r -> groupBy.keySet().stream().map(r::get)).distinct().count() == rows.size();
	}
	
}