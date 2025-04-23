package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.PersistenceMemory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Prevent usage of {@link PersistenceMemory} for this class.
 *
 * @see Memorized
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Fresh {

}