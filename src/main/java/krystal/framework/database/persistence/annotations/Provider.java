package krystal.framework.database.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a {@link krystal.framework.database.abstraction.ProviderInterface ProviderInterface} used for database operations. Annotate either a field or a method, returning that type.
 *
 * @see krystal.framework.database.abstraction.ProviderInterface ProviderInterface
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {

}