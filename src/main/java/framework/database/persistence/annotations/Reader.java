package framework.database.persistence.annotations;

import framework.database.abstraction.QueryResultInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks constructor used to parse data from {@link framework.database.abstraction.QueryResultInterface QueryResultInterface}. The constructor's parameters types must match the columns types, as well as their order. Use custom methods to map incoming
 * values with actual fields types, by matching i.e. <i><b>@AllArgsConstructor</b></i>.
 *
 * @see framework.database.persistence.PersistenceInterface#mapQueryResult(QueryResultInterface, Class) PersistenceInterface.mapQueryResult()
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface Reader {

}