package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

class FieldContextPropertyInjection extends AbstractContextPropertyInjector {
    private final int paramIndex;
    private final IOpenField field;
    private final IOpenCast openCast;

    public FieldContextPropertyInjection(int paramIndex, IOpenField field, IOpenCast openCast) {
        this.paramIndex = paramIndex;
        this.field = field;
        this.openCast = openCast;
    }

    @Override
    protected Object getValue(Object[] params, IRuntimeEnv env) {
        Object value = field.get(params[paramIndex], env);
        return openCast.convert(value);
    }

    @Override
    protected boolean isProcessable(Object[] params) {
        return params[paramIndex] != null;
    }

    @Override
    protected String getContextProperty() {
        return field.getContextProperty();
    }
}
