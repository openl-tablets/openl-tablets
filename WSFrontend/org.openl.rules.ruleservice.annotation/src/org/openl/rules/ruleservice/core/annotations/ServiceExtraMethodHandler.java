package org.openl.rules.ruleservice.core.annotations;

import java.lang.reflect.Method;

public interface ServiceExtraMethodHandler<T> {
    T invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception;
}
