package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To hint a type for runtime generated classes.
 * 
 * <pre>
 * {@code
 * interface Service {
 *     @RulesType("Rating") Object calculate(@RulesType("Policy") Object policy);
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.METHOD })
public @interface RulesType {
    String value();

    int arrayDims() default -1;
}
