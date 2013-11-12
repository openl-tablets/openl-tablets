package org.openl.rules.testmethod;

import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class ParameterWithValueDeclaration extends ParameterDeclaration {
    private Object value;
    
    public ParameterWithValueDeclaration(String paramName, Object value, IOpenClass parameterType, int direction) {
        super(parameterType, paramName, direction);
        this.value = value;
    }
    
    public ParameterWithValueDeclaration(String paramName, Object value, int direction) {        
        super(getParamType(value), paramName, direction);
        this.value = value;
    }
    
    public static IOpenClass getParamType(Object value) {
        if (value == null) {
            return NullOpenClass.the;
        } else {
            return JavaOpenClass.getOpenClass(value.getClass());
        }
    }

    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
}
