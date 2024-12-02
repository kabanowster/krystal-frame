package krystal.framework.database.persistence.annotations;

import krystal.framework.database.persistence.Persistence;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.PersistenceMemory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link PersistenceMemory} will hold objects of this class infinitely unless explicitly cleared. {@link Persistence} and {@link PersistenceInterface} operations, besides database, will also overwrite or remove the memorized records.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Memorized {

}