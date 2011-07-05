package org.openl.rules.tutorial4;

import java.lang.reflect.Method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.generated.beans.Driver;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodBeforeInterceptor;

public class DriverValidator extends ServiceMethodBeforeInterceptor {

    public DriverValidator(Method method) {
        super(method);
    }

    public void invoke(Object... args) {
        for (int i = 0; i < getMethod().getParameterTypes().length; i++) {
            if (getMethod().getParameterTypes()[i].equals(Driver.class)) {
                Driver driver = (Driver) args[i];
                if (driver == null || driver.getName() == null) {
                    throw new OpenLRuntimeException("Driver name connot be null.");
                }
                return;
            }
        }
    }
}
