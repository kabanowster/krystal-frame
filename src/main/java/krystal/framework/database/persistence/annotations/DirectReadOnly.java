package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark Class that is read-only and only through {@link PersistenceInterface#mapQueryResult(QueryResultInterface, Class) PersistenceInterface.mapQueryResult()} or {@link QueryResultInterface#toStreamOf(Class) QueryResultInterface.toStreamOf()} methods.
 * Used mainly to inform and within {@link PersistenceInterface PersistenceInterface} validation. Class can be loaded and mapped from database with only single {@link Reader @Reader} present.
 *
 * @see PersistenceInterface PersistenceInterface
 * @see QueryResultInterface
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DirectReadOnly {

}