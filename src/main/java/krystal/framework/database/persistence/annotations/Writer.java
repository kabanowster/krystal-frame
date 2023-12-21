package krystal.framework.database.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use to mark methods that are writers. Writers are mappings for fields to output desired values and types when writing to database. Writer is defined by creating a method declared as <i><b>writeFieldName()</b></i>, returning desired value and type. This
 * annotation is available just for convenience - the writer is distinguished by the method's name only.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Writer {

}