package krystal.framework.database.persistence.annotations;

import krystal.framework.database.queryfactory.ColumnsComparisonInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If applied, the presence of any {@link Key @Key} field passes the key presence check for persistence operation. Otherwise, <b>all</b> keys are required to be set.
 * In other words, any of declared keys is able to point to the specific object.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SeparateKeys {
	
	/**
	 * If {@code true}, the missing keys will be included in the query with {@code IS NULL} value, when building {@link ColumnsComparisonInterface} for persistence operation SQL. False by default.
	 */
	boolean includeIfNull() default false;
	
}