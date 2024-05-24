package krystal.framework.database.persistence;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.persistence.annotations.ColumnsMapping;
import krystal.framework.database.persistence.annotations.Key;
import krystal.framework.database.persistence.annotations.Vertical;
import lombok.*;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Use this builder to create a map of fields to columns if they have different names.
 * Get a field using <b><i>this.fld(fieldName)</i></b> method.
 * Use as method returning this type or {@link Enum} implementing {@link ColumnInterface}, all within {@link PersistenceInterface} class and marked with {@link ColumnsMapping}.
 * In case of {@link Vertical @Vertical} class, the columns mapping for {@link Key non-@Key} fields becomes their name within {@link Vertical.PivotColumn} values.
 *
 * @see ColumnsMapping
 * @see #fromColumnInterfaceEnum(Class, Class)
 * @see #formatAll(Class, String)
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
	
	/**
	 * Declare {@link Predicate} for {@link #allMatchingToSingle(Class, ColumnInterface, Predicate) allMatchingToSingle()} method, as all fields not annotated by {@link PersistenceInterface#getPersistenceSetupAnnotations()}.
	 */
	public static ColumnsMap allNonPersistenceToSingle(Class<?> clazz, ColumnInterface column) {
		return allMatchingToSingle(clazz, column, f -> PersistenceInterface.getPersistenceSetupAnnotations().stream().noneMatch(f::isAnnotationPresent));
	}
	
	/**
	 * Point all matching {@link Field fields} of the class to a single {@link ColumnInterface}.
	 */
	public static ColumnsMap allMatchingToSingle(Class<?> clazz, ColumnInterface column, Predicate<Field> predicate) {
		return new ColumnsMap(Arrays.stream(clazz.getDeclaredFields())
		                            .filter(predicate)
		                            .collect(Collectors.toMap(f -> f, f -> column, (f1, f2) -> f1, HashMap::new)));
	}
	
	/**
	 * Build ColumnsMap from provided Enum which implements {@link ColumnInterface}, which constants represent class' {@link Field fields} names.
	 */
	public static <E extends Enum<?> & ColumnInterface> ColumnsMap fromColumnInterfaceEnum(Class<?> clazz, @NonNull Class<E> columns) {
		val fields = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toMap(Field::getName, f -> f));
		val builder = ColumnsMap.define();
		Arrays.stream(columns.getEnumConstants()).forEach(c -> builder.column(Objects.requireNonNull(fields.get(c.name())), c));
		return builder.set();
	}
	
	/**
	 * Type in the format, that will be issued for each field name in a class (using <i>String.format()</i>) and returned as {@link ColumnInterface}.
	 */
	public static ColumnsMap formatAll(Class<?> clazz, String format) {
		val map = ColumnsMap.define();
		Arrays.stream(clazz.getDeclaredFields()).forEach(f -> map.column(f, () -> format.formatted(f.getName())));
		return map.set();
	}
	
}