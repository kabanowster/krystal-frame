package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ColumnInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a database {@link ColumnInterface} for a field if their names differ. Can be used directly on field instead defining separate {@link ColumnsMapping}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	
	String value();
	
}