package org.openl.rules.testmethod;

import org.openl.rules.table.formatters.FormattersManager;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.util.formatters.IFormatter;

public class TestDescription {
    private ExecutionParamDescription[] executionParams;
    private IOpenMethod testedMethod;
    private DynamicObject testArgs;

    public TestDescription(IOpenMethod testedMethod, DynamicObject testArgs) {
        this.testedMethod = testedMethod;
        this.testArgs = testArgs;
    }

    public ExecutionParamDescription[] getExecutionParams() {
        if (executionParams == null) {
            initExecutionParams();
        }
        return executionParams;
    }

    private void initExecutionParams() {
        executionParams = new ExecutionParamDescription[testedMethod.getSignature().getNumberOfParameters()];
        for (int i = 0; i < executionParams.length; i++) {
            String paramName = testedMethod.getSignature().getParameterName(i);
            Object paramValue = testArgs.getFieldValue(paramName);
            executionParams[i] = new ExecutionParamDescription(paramName, paramValue);
        }
    }

    @Override
    public String toString() {
        String description = (String) testArgs.getFieldValue(TestMethodHelper.DESCRIPTION_NAME);

        if (description == null) {
            if (testedMethod.getSignature().getNumberOfParameters() > 0) {
                String name = testedMethod.getSignature().getParameterName(0);
                Object value = testArgs.getFieldValue(name);
                IFormatter formatter = FormattersManager.getFormatter(value);
                description = formatter.format(value);
            } else {
                description = "Run with no parameters";
            }
        }
        return description;
    }

}
