package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.ColumnsMap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link ColumnsMap} to load the object with. Annotate either a field or a method, returning that type.
 *
 * @see ColumnsMap
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnsMapping {

}