package krystal.framework.database.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If applied, the presence of any {@link Key @Key} field passes the key presence check for persistence operation. Otherwise, <b>all</b> keys are required to be set.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SeparateKeys {

}