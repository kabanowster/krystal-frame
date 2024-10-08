package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks constructor used to parse data from {@link QueryResultInterface QueryResultInterface}. The constructor's parameters types must match the {@link ColumnInterface columns} types, as well as their order. Use custom methods to map incoming values with
 * actual fields types, by matching i.e. {@link lombok.AllArgsConstructor @AllArgsConstructor}.
 * <p>
 * Methods of type {@code void}, marked with this annotation, will be invoked after the constructor.
 *
 * @see PersistenceInterface#mapQueryResult(QueryResultInterface, Class) PersistenceInterface.mapQueryResult()
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Reader {

}