package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.Persistence;
import krystal.framework.database.persistence.PersistenceMemory;
import krystal.framework.database.persistence.filters.Filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If and only if the {@link PersistenceMemory} holds entries of any instances of this class, the database load is skipped when invoking {@link Persistence#promiseAll(Class, Filter)}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Memorized {

}