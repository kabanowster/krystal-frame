package krystal.framework.database.implementation;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import krystal.StringRenderer;
import krystal.framework.database.abstraction.ColumnInterface;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record QueryResultRow(Map<ColumnInterface, Object> row, Map<ColumnInterface, Class<?>> columns, int resultHash) {
	
	public QueryResultRow(Row row, RowMetadata metadata, int resultHash) {
		this(LinkedHashMap.newLinkedHashMap(metadata.getColumnMetadatas().size()), LinkedHashMap.newLinkedHashMap(metadata.getColumnMetadatas().size()), resultHash);
		
		columns.putAll(metadata.getColumnMetadatas().stream().collect(Collectors.toMap(
				m -> ColumnInterface.of(m.getName()),
				m -> Objects.requireNonNullElse(m.getJavaType(), Object.class),
				(a, b) -> a,
				() -> new LinkedHashMap<ColumnInterface, Class<?>>() // WTF can not lambda
		)));
		
		Map<ColumnInterface, Object> values = LinkedHashMap.newLinkedHashMap(columns.size());
		columns.forEach((col, clazz) -> {
			values.put(col, row.get(col.sqlName(), clazz));
		});
		row().putAll(values);
		
	}
	
	@Override
	public String toString() {
		return "\nHash: %s".formatted(this.resultHash) +
				StringRenderer.renderTable(
						columns.entrySet().stream()
						       .map(e -> String.format("%s (%s)", e.getKey().sqlName(), e.getValue().getSimpleName()))
						       .toList(),
						List.of(row.values().stream().map(String::valueOf).toList())
				);
	}
	
}