package org.openl.rules.calc;

import java.util.Objects;

import org.openl.base.INamedThing;
import org.openl.rules.binding.RecursiveSpreadsheetMethodPreBindingException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultField extends ASpreadsheetField {

    private IOpenField field;

    public CustomSpreadsheetResultField(CustomSpreadsheetResultOpenClass declaringClass, IOpenField field) {
        super(declaringClass, field.getName(), null);
        this.field = Objects.requireNonNull(field, "field cannot be null");
    }

    public CustomSpreadsheetResultField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (field != null) {
            throw new IllegalStateException("Spreadsheet cell type is not resolved at compile time");
        }
        if (target == null) {
            return getType().nullObject();
        }
        Object res = ((SpreadsheetResult) target).getFieldValue(getName());
        return processResult(res);
    }

    @Override
    public IOpenClass getType() {
        if (field != null) {
            // Lazy initialization for cells level recursive compilation
            try {
                setType(field.getType());
            } catch (RecursiveSpreadsheetMethodPreBindingException | SpreadsheetCellsLoopException e) {
                setType(JavaOpenClass.OBJECT);
            }
            field = null;
        }
        return super.getType();
    }

    protected Object processResult(Object res) {
        if (res != null && !ClassUtils.isAssignable(res.getClass(), getType().getInstanceClass())) {
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
        if (target != null) {
            ((SpreadsheetResult) target).setFieldValue(getName(), value);
        }
    }

}