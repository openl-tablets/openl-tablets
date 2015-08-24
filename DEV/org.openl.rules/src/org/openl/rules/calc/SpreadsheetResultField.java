package org.openl.rules.calc;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetResultField extends AOpenField {
    private IOpenClass declaringClass;

    public SpreadsheetResultField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(name, type);
        this.declaringClass = declaringClass;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) target;
        String name = getName();

        if (!spreadsheetResult.hasField(name)) {
            throw new OpenLRuntimeException(String.format("Field '%s' does not exist in SpreadsheetResult", name));
        }

        Object res = spreadsheetResult.getFieldValue(name);
        return res != null ? res : getType().nullObject();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) target;
        String name = getName();

        if (!spreadsheetResult.hasField(name)) {
            throw new OpenLRuntimeException(String.format("Field '%s' does not exist in SpreadsheetResult", name));
        }
        Point fieldCoordinates = spreadsheetResult.getFieldsCoordinates().get(name);
        spreadsheetResult.setValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn(), value);
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

}
