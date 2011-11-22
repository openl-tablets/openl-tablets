package org.openl.rules.testmethod;

import org.openl.types.IOpenClass;

public class ExecutionParamDescriptionWithType extends ExecutionParamDescription {
    private IOpenClass parameterType;

    public ExecutionParamDescriptionWithType(String paramName, Object value, IOpenClass parameterType) {
        super(paramName, value);
        this.parameterType = parameterType;
    }

    @Override
    public IOpenClass getParamType() {
        return parameterType;
    }
}
