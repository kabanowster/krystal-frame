package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.persistence.ColumnsMap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link ColumnsMap} which is used to translate object's fields to {@link ColumnInterface}.
 * Annotate either a method, returning that type or {@link Enum} implementing {@link ColumnInterface}, or mark field with {@link Column}.
 * With method annotation, you can define more complex, conditional mappings.
 *
 * @see Column
 * @see ColumnsMap
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnsMapping {

}