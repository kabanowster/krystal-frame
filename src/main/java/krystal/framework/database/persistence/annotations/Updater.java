package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.Query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method that returns {@link Query} which is used as the explicit statement during update execution. This can be, i.e., a stored procedure.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Updater {

}