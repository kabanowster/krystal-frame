package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deletion equivalent of the {@link Writer} void methods, as last step of {@link PersistenceInterface#delete()} execution.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Remover {

}