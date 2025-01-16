package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link ProviderInterface} used for database operations. Annotate a method, returning that type.
 *
 * @see PersistenceInterface#getProvider()
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {

}