package org.openl.rules.calc;

import org.openl.types.IOpenClass;
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

        return processResult(spreadsheetResult, res);
    }

    public Object processResult(SpreadsheetResult spreadsheetResult, Object res) {
        if (res != null && spreadsheetResult.getCustomSpreadsheetResultOpenClass() != null && !getType()
            .getInstanceClass()
            .isAssignableFrom(res.getClass())) {
            throw new UnexpectedSpreadsheetResultFieldTypeException(
                String.format("Unexpected type for field %s in %s. Expected: %s, found: %s",
                    getName(),
                    getDeclaringClass().getName(),
                    spreadsheetResult.getCustomSpreadsheetResultOpenClass().getName(),
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