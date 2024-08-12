package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a method returning {@code Function<SelectStatement, WhereClause>} modifying {@link Loader @Loader} statement (either generic or custom).
 *
 * @see PersistenceInterface#getFilterQuery(Class, Object)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Filter {

}