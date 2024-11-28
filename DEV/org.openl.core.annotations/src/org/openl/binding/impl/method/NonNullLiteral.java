package org.openl.binding.impl.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used by method search algorithm. If the method is called with null literal for the method
 * parameter marked with this annotation then the method matching is not happened.
 * <p>
 * It can be useful for cases:
 * <p>
 * add(T[], T) add(T[], T...)
 * <p>
 * If method add(a, null) is called then the method add(T[], T...) is selected as most specific, but if we mark the
 * annotation the second parameter of the method add(T[], @NonNullLiteral T...) then the method add(T[], T) is selected
 * because first one is ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.PARAMETER})
public @interface NonNullLiteral {
}
