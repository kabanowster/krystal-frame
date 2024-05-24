package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.queryfactory.SelectStatement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a custom {@link SelectStatement SelectStatement} to load the object with. Annotate a method, returning that type or {@link Enum} implementing {@link ColumnInterface}, where in second case, the statement will be constructed using object's
 * {@link PersistenceInterface#getTable() table} and provided constants as columns to get.
 * By default, (if this annotation is missing), the object is loaded with {@code SELECT * FROM }{@link PersistenceInterface#getTable() table} query.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loader {

}