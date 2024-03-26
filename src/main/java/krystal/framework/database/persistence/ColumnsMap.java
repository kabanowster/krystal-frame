package krystal.framework.database.persistence;

import krystal.framework.database.abstraction.ColumnInterface;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Use this builder to create a map of fields to columns if they have different names. Get a field using <b><i>this.fld(fieldName)</i></b> method.
 * Implement as method returning this type, within {@link PersistenceInterface} class.
 *
 * @see PersistenceInterface#formatAll(String)
 * @see PersistenceInterface#fld(String)
 */
@Builder(builderMethodName = "define", buildMethodName = "set")
@Getter
@Accessors(fluent = true)
public class ColumnsMap {
	
	private @Singular Map<Field, ColumnInterface> columns;
	
	public static ColumnsMap empty() {
		return new ColumnsMap(Map.of());
	}
	
}