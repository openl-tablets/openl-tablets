package org.openl.rules.ruleservice.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If service method parameter is marked with @{@link NoTypeConversion} then type conversation must be avoided.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface NoTypeConversion {
}
