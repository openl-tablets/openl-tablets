package org.openl.binding.impl.cast;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is designed to be used on java method to extend the behaviour of the linkage process by
 * {@link org.openl.binding.impl.method.MethodSearch} algorithm.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodSearchTuner {
    Class<? extends MethodCallerWrapper> wrapper() default DefaultMethodCallerWrapper.class;

    Class<? extends MethodFilter> methodFilter() default DefaultMethodFilter.class;

    abstract class DefaultMethodCallerWrapper implements MethodCallerWrapper {
    }

    abstract class DefaultMethodFilter implements MethodFilter {
    }
}
