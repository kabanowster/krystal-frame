package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a custom {@link krystal.framework.database.queryfactory.SelectStatement SelectStatement} to load the object with. Annotate either a field or a method, returning that type.
 * By default (if this annotation is missing), the object is loaded with {@code SELECT * FROM }{@link PersistenceInterface#getTable() table} query.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loader {

}