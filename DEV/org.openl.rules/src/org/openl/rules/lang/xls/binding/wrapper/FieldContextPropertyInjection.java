package org.openl.rules.lang.xls.binding.wrapper;

import lombok.RequiredArgsConstructor;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

@RequiredArgsConstructor
class FieldContextPropertyInjection extends AbstractContextPropertyInjector {
    private final int paramIndex;
    private final IOpenField field;
    private final IOpenCast openCast;

    @Override
    protected Object getValue(Object[] params, IRuntimeEnv env) {
        var value = field.get(params[paramIndex], env);
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
