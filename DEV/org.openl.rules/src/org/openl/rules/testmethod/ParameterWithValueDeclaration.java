package org.openl.rules.testmethod;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class ParameterWithValueDeclaration extends ParameterDeclaration implements IParameterWithValueDeclaration {
    private Object value;

    /**
     * The key field for the value. For example firstName for a Driver. Can be first field for an object. For now the
     * field is used instead of calculated field value. It's needed to handle arrays of objects: for an array of Driver
     * key field (foreign key) will be firstName, not array index. So array of Driver can be displayed as array of first
     * names instead of array of big Driver objects. Can be refactored later if such cases will be handled correctly.
     */
    private IOpenField keyField;

    public ParameterWithValueDeclaration(String paramName,
            Object value,
            IOpenClass parameterType,
            IOpenField keyField) {
        super(parameterType, paramName);
        this.value = value;
        this.keyField = keyField;
    }

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

    public IOpenField getKeyField() {
        return keyField;
    }
}
