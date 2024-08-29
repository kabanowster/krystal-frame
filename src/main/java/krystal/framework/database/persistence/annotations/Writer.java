package krystal.framework.database.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use to mark methods that are writers - mapping methods for fields to return desired values and types when saving to the database. It is defined by creating a method declared as {@code writeFieldName()}, or by using this annotation with
 * the field's name as parameter. Methods of type {@code void} can be marked without parameter, and will be invoked as the last step of Persistence saving execution, thus creating additional writing logic.
 *
 * @see Remover
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Writer {
	
	String fieldName() default "";
	
}