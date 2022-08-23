package org.openl.rules.ruleservice.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is designed for marking a service method parameters as non a rule parameter. All marked parameters is
 * not passed to the rule, but can be used by other parts of the system. For example: CXF, interceptors etc.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ExternalParam {
}
