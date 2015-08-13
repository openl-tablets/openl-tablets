package org.openl.rules.testmethod;

import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class ParameterWithValueDeclaration extends ParameterDeclaration implements IParameterWithValueDeclaration {
    private Object value;

    public ParameterWithValueDeclaration(String paramName, Object value, IOpenClass parameterType) {
        super(parameterType, paramName);
        this.value = value;
    }

    public ParameterWithValueDeclaration(String paramName, Object value) {
        super(getParamType(value), paramName);
        this.value = value;
    }

    public static IOpenClass getParamType(Object value) {
        if (value == null) {
            return NullOpenClass.the;
        } else {
            return JavaOpenClass.getOpenClass(value.getClass());
        }
    }

    @Override
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
