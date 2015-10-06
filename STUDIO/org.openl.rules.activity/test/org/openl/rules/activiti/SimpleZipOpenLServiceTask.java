package org.openl.rules.activiti;

import java.lang.reflect.Method;

import org.activiti.engine.delegate.DelegateExecution;
import org.openl.meta.DoubleValue;

public class SimpleZipOpenLServiceTask extends AbstractOpenLResourceServiceTask<Object> {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String driverAge = (String) execution.getVariable("driverAge");

        String driverMatrialStatus = (String) execution.getVariable("driverMaritalStatus");
        Object instance = getInstance(execution);

        Class<?> clazz = getSimpleProjectEngineFactory(execution).getInterfaceClass();
        Method method = clazz.getMethod("DriverPremium1", String.class, String.class);

        DoubleValue result = (DoubleValue) method.invoke(instance, new Object[] { driverAge, driverMatrialStatus });

        execution.setVariable("resultVariable", result);
    }
}
