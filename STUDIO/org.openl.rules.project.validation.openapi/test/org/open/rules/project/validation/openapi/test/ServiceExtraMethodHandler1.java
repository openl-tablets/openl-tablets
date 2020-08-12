package org.open.rules.project.validation.openapi.test;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;

public class ServiceExtraMethodHandler1 implements ServiceExtraMethodHandler<Double> {
    @Override
    public Double invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        return 0d;
    }
}
