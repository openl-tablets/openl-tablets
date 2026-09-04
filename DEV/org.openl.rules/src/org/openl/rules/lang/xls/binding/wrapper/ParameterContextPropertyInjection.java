package org.openl.rules.lang.xls.binding.wrapper;

import lombok.Getter;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.vm.IRuntimeEnv;

class ParameterContextPropertyInjection extends AbstractContextPropertyInjector {
    private final int paramIndex;
    private final IOpenCast openCast;
    @Getter
    private final String contextProperty;

    public ParameterContextPropertyInjection(int paramIndex, String contextProperty, IOpenCast openCast) {
        this.paramIndex = paramIndex;
        this.openCast = openCast;
        this.contextProperty = contextProperty;
    }

    @Override
    protected Object getValue(Object[] params, IRuntimeEnv env) {
        var value = params[paramIndex];
        return openCast.convert(value);
    }

    @Override
    protected boolean isProcessable(Object[] params) {
        return true;
    }
}
