package org.openl.rules.calc;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeDescriptionHolder;
import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetResultField extends AOpenField implements NodeDescriptionHolder {
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
            return getType().nullObject();
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
        spreadsheetResult.setFieldValue(name, value);
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(IOpenClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    @Override
    public String getDescription() {
        return "Spreadsheet\n" + getType().getDisplayName(INamedThing.SHORT) + " " + getName();
    }
}
