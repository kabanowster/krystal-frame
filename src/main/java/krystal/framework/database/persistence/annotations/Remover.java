package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.Query;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method returning {@link Query} which is used as the explicit statement during {@link PersistenceInterface#delete()} execution. This can be, i.e., a stored procedure.
 * Void {@link Remover} methods are run as the <b><i>first</i></b> step of {@link PersistenceInterface#delete()} execution.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Remover {

}