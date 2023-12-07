package framework.database.persistence;

import framework.database.abstraction.ColumnInterface;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;

import java.util.Map;

@Builder(builderMethodName = "define", buildMethodName = "set")
@Getter
@Accessors(fluent = true)
public class ColumnsMap {
	
	private @Singular Map<String, ColumnInterface> columns;
	
	public static ColumnsMap empty() {
		return new ColumnsMap(Map.of());
	}
	
}