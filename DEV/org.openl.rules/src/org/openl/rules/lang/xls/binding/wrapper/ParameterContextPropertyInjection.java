package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.vm.IRuntimeEnv;

class ParameterContextPropertyInjection extends AbstractContextPropertyInjector {
    private final int paramIndex;
    private final IOpenCast openCast;
    private final String contextProperty;

    public ParameterContextPropertyInjection(int paramIndex, String contextProperty, IOpenCast openCast) {
        this.paramIndex = paramIndex;
        this.openCast = openCast;
        this.contextProperty = contextProperty;
    }

    @Override
    protected Object getValue(Object[] params, IRuntimeEnv env) {
        Object value = params[paramIndex];
        return openCast.convert(value);
    }

    @Override
    public String getContextProperty() {
        return contextProperty;
    }

    @Override
    protected boolean isProcessable(Object[] params) {
        return true;
    }
}
