package org.openl.rules.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field of the Bean that this field is a context property.
 * Context properties are used to organize rules versioning by some dimensions.
 *
 * @author Yury Molchan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.METHOD})
public @interface ContextProperty {
    String value() default "";
}
