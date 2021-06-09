package org.example.services;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;

public class EPBDS10595ServiceExtraMethodHandler implements ServiceExtraMethodHandler<Object> {
    @Override
    public Object invoke(Method interfaceMethod, Object serviceBean, Object... args) {
        return null;
    }
}
