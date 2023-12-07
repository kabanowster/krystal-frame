package framework.database.persistence.annotations;

import framework.database.abstraction.QueryResultInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark Class that is read-only and only through {@link framework.database.persistence.PersistenceInterface#mapQueryResult(QueryResultInterface, Class) PersistenceInterface.mapQueryResult()} or
 * {@link QueryResultInterface#toStreamOf(Class) QueryResultInterface.toStreamOf()} methods. Used mainly to inform and within {@link framework.database.persistence.PersistenceInterface PersistenceInterface} validation. Class can be loaded and mapped
 * from database with only single {@link Reader @Reader} present.
 *
 * @see framework.database.persistence.PersistenceInterface PersistenceInterface
 * @see QueryResultInterface
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DirectReadOnly {

}