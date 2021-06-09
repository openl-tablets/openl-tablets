package org.openl.rules.calc;

import java.util.Objects;

import org.openl.base.INamedThing;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.binding.RecursiveSpreadsheetMethodPreBindingException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomSpreadsheetResultField extends ASpreadsheetField implements IOriginalDeclaredClassesOpenField {
    private static final Logger LOG = LoggerFactory.getLogger(CustomSpreadsheetResultField.class);
    protected IOpenField field;
    private final IOpenClass[] declaringClasses;

    public CustomSpreadsheetResultField(CustomSpreadsheetResultOpenClass declaringClass, IOpenField field) {
        super(declaringClass, field.getName(), null);
        this.field = Objects.requireNonNull(field, "field cannot be null");
        this.declaringClasses = new IOpenClass[] { declaringClass };
    }

    public CustomSpreadsheetResultField(IOpenClass declaringClass, String name, IOpenClass type) {
        super(declaringClass, name, type);
        this.declaringClasses = new IOpenClass[] { declaringClass };
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
                LOG.debug("Error occurred: ", e);
                setType(JavaOpenClass.OBJECT);
            }
            field = null;
        }
        return super.getType();
    }

    protected Object processResult(Object res) {
        if (res != null) {
            if (!ClassUtils.isAssignable(res.getClass(), getType().getInstanceClass())) {
                return convertWithFailSafeCast(res);
            }
            return res;
        } else {
            return getType().nullObject();
        }
    }

    protected final Object convertWithFailSafeCast(Object res) {
        IOpenCast cast = ((CustomSpreadsheetResultOpenClass) getDeclaringClass()).getModule()
            .getObjectToDataOpenCastConvertor()
            .getConvertor(res.getClass(), getType().getInstanceClass());
        if (cast != null && cast.isImplicit()) {
            return cast.convert(res);
        } else {
            throw new UnexpectedSpreadsheetResultFieldTypeException(
                String.format("Unexpected type for field '%s' in '%s'. Expected type '%s', but found '%s'.",
                    getName(),
                    getDeclaringClass().getName(),
                    getType().getDisplayName(INamedThing.LONG),
                    res.getClass().getTypeName()));
        }
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

    @Override
    public IOpenClass[] getDeclaringClasses() {
        return declaringClasses;
    }

}