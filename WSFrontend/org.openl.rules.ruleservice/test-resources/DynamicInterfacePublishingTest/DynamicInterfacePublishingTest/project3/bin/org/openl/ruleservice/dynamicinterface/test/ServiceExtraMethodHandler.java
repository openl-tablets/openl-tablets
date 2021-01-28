package org.openl.ruleservice.dynamicinterface.test;

import java.lang.reflect.Method;

public class ServiceExtraMethodHandler implements org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler<String> {
    @Override
    public String invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        return "Hello world ServiceExtraMethodHandler!";
    }
}
