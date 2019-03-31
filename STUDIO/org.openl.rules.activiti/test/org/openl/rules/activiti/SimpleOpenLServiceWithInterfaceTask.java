package org.openl.rules.activiti;

import org.activiti.engine.delegate.DelegateExecution;
import org.openl.meta.DoubleValue;

public class SimpleOpenLServiceWithInterfaceTask extends AbstractOpenLResourceServiceTask<RulesInterface> {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String driverAge = (String) execution.getVariable("driverAge");

        String driverMatrialStatus = (String) execution.getVariable("driverMaritalStatus");
        RulesInterface instance = getInstance(execution);

        DoubleValue result = instance.DriverPremium1(driverAge, driverMatrialStatus);

        execution.setVariable("resultVariable", result);
    }
}
