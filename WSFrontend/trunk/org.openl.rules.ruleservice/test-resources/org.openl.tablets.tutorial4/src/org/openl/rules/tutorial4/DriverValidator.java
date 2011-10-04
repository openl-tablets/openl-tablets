package org.openl.rules.tutorial4;

import java.lang.reflect.Method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.generated.beans.Driver;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeAdvice;

public class DriverValidator implements ServiceMethodBeforeAdvice {

    @Override
    public void before(Method method, Object proxy, Object... args) throws Throwable {
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (method.getParameterTypes()[i].equals(Driver.class)) {
                Driver driver = (Driver) args[i];
                if (driver == null || driver.getName() == null) {
                    throw new OpenLRuntimeException("Driver name connot be null.");
                }
                return;
            }
        }
    }

}
