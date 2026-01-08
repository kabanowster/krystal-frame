package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method that returns {@link Query} which is used as the explicit statement during insert execution. This can be, i.e., a stored procedure.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inserter {
	
	/**
	 * Query outputs values to be parsed with {@link Reader}, updating the object with incoming data. I.e., values for {@link Key}. Only available if {@link DBCDriverInterface} for selected {@link ProviderInterface} supports this operation.
	 */
	boolean withOutput() default false;
}