package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * Mark Class that is read-only, which basically restricts all saving operations, but allows some of the {@link PersistenceInterface PersistenceInterface} loading methods like
 * {@link PersistenceInterface#streamAll(Class, QueryExecutorInterface, Function, Object)}  streamAll()}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {

}