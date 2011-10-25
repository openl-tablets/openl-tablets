package org.openl.rules.testmethod;

import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class ExecutionParamDescription {
    private String paramName;
    private Object value;

    public ExecutionParamDescription(String paramName, Object value) {
        this.paramName = paramName;
        this.value = value;
    }

    public String getParamName() {
        return paramName;
    }

    public IOpenClass getParamType() {
        if (value == null) {
            return NullOpenClass.the;
        } else {
            return JavaOpenClass.getOpenClass(value.getClass());
        }
    }

    public Object getValue() {
        return value;
    }
}
