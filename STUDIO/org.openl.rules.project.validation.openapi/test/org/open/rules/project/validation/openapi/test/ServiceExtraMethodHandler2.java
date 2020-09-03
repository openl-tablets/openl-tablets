package org.open.rules.project.validation.openapi.test;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;

public final class ServiceExtraMethodHandler2 implements ServiceExtraMethodHandler<String> {
    @Override
    public String invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        return null;
    }
}
