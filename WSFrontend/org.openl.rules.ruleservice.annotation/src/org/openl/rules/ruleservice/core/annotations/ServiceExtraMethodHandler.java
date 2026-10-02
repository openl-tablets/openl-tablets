package org.openl.rules.ruleservice.core.annotations;

import java.lang.reflect.Method;

public interface ServiceExtraMethodHandler<T> {
    // Customer extension point: an implementation may throw any exception, which reaches the service caller.
    @SuppressWarnings("java:S112")
    T invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception;
}
