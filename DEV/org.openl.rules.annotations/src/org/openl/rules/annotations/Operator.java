package org.openl.rules.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To mark a static methods that they are registered as OpenL operators.
 * OpenL operators are named methods for java symbols, like add, mul, le, eq, not, cast...
 *
 * @author Yury Molchan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Operator {

}
