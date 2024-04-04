package krystal.framework.database.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link krystal.framework.database.persistence.ColumnsMap ColumnsMapping} to load the object with. Annotate either a field or a method, returning that type.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnsMapping {

}