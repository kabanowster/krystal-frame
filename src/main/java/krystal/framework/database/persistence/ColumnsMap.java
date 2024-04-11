package krystal.framework.database.persistence;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.persistence.annotations.Key;
import krystal.framework.database.persistence.annotations.Vertical;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Use this builder to create a map of fields to columns if they have different names. Get a field using <b><i>this.fld(fieldName)</i></b> method.
 * Implement as method returning this type, within {@link PersistenceInterface} class.
 * In case of {@link Vertical @Vertical} class, the columns mapping for {@link Key non-@Key} fields becomes their name within {@link Vertical.PivotColumn} values.
 *
 * @see PersistenceInterface#formatAll(String)
 * @see PersistenceInterface#fld(String)
 */
@Builder(builderMethodName = "define", buildMethodName = "set", toBuilder = true)
@Getter
@Accessors(fluent = true)
public class ColumnsMap {
	
	private @Singular Map<Field, ColumnInterface> columns;
	
	public static ColumnsMap empty() {
		return new ColumnsMap(Map.of());
	}
	
	public static ColumnsMap allNonPersistenceToSingle(Class<?> clazz, ColumnInterface column) {
		return allMatchingToSingle(clazz, column, f -> PersistenceInterface.getPersistenceSetupAnnotations().stream().noneMatch(f::isAnnotationPresent));
	}
	
	public static ColumnsMap allMatchingToSingle(Class<?> clazz, ColumnInterface column, Predicate<Field> predicate) {
		return new ColumnsMap(Arrays.stream(clazz.getDeclaredFields())
		                            .filter(predicate)
		                            .collect(Collectors.toMap(f -> f, f -> column, (f1, f2) -> f1, HashMap::new)));
	}
	
}