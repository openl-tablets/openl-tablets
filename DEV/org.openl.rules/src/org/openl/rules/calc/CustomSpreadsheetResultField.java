package org.openl.rules.calc;

import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultField extends ASpreadsheetField {

    public CustomSpreadsheetResultField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }

        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) target;

        Object res = spreadsheetResult.getFieldValue(getName());

        return processResult(res);
    }

    protected Object processResult(Object res) {
        if (res != null && (getType().getInstanceClass() == null || !ClassUtils.isAssignable(res.getClass(),
            getType().getInstanceClass()))) {
            throw new UnexpectedSpreadsheetResultFieldTypeException(
                String.format("Unexpected type for field '%s' in '%s'. Expected type '%s', but found '%s'.",
                    getName(),
                    getDeclaringClass().getName(),
                    getType().getDisplayName(INamedThing.LONG),
                    res.getClass().getTypeName()));
        }

        return res != null ? res : getType().nullObject();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) target;
        spreadsheetResult.setFieldValue(getName(), value);
    }

}