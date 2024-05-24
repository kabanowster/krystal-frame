package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.persistence.ColumnsMap;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link ColumnsMap} which is used to translate object's fields to {@link ColumnInterface} (in reverse actions to {@link Reader @Reader}). I.e. when using {@link PersistenceInterface#load()} method with {@link Key @Key} fields set for empty
 * object.
 * Annotate either a method, returning that type or {@link Enum} implementing {@link ColumnInterface}.
 *
 * @see ColumnsMap
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnsMapping {

}