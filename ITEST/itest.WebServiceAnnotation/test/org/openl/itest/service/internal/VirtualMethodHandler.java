package org.openl.itest.service.internal;

import java.lang.reflect.Method;

import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethodHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualMethodHandler implements ServiceExtraMethodHandler<Double> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public Double invoke(Method interfaceMethod, Object serviceBean, Object... args) throws Exception {
        log.debug("Invoking " + interfaceMethod.getName() + "( '" + args[0] + "' )");

        Method method = serviceBean.getClass().getMethod("parse", String.class);

        log.debug("Redirecting to parse( '" + args[0] + "' )");

        Object result = method.invoke(serviceBean, args[0]);

        log.debug("Result is " + result);

        Integer num = (Integer) result;
        Double value = num * 1.5;

        log.debug("Converted to " + value);

        return value;
    }

}
