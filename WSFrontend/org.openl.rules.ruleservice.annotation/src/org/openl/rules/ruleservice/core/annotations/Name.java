package org.openl.rules.ruleservice.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * EPBDS-6736 This annotation is used together with @{@link ServiceExtraMethod} to define 'pretty' names REST services
 * instead of 'arg0', 'arg1'...
 *
 * It needs because Java byte code does not store arguments names in interfaces.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Name {
    String value();
}
