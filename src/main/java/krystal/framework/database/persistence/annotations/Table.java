package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.persistence.Entity;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a database {@link TableInterface} for a {@link Entity} class.
 *
 * @apiNote You can also directly override {@link PersistenceInterface#getTable()} method.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
	
	String value();
	
}