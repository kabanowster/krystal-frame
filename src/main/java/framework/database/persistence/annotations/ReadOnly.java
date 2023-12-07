package framework.database.persistence.annotations;

import framework.database.abstraction.QueryExecutorInterface;
import framework.database.abstraction.QueryResultInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark Class that is read-only which basically restricts all saving operations, but can use some of the
 * {@link framework.database.persistence.PersistenceInterface PersistenceInterface} methods like
 * {@link framework.database.persistence.PersistenceInterface#getAll(Class, QueryExecutorInterface) getAll()}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {

}