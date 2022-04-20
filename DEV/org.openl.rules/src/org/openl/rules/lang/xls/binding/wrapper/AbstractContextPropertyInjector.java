package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.vm.IRuntimeEnv;

abstract class AbstractContextPropertyInjector implements IContextPropertyInjection {

    public IRulesRuntimeContext inject(Object[] params,
            IRuntimeEnv env,
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv,
            IRulesRuntimeContext rulesRuntimeContext) {
        if (isProcessable(params)) {
            Object value = getValue(params, env);
            if (rulesRuntimeContext == null) {
                IRulesRuntimeContext currentRuntimeContext = (IRulesRuntimeContext) simpleRulesRuntimeEnv.getContext();
                try {
                    rulesRuntimeContext = currentRuntimeContext.clone();
                    rulesRuntimeContext.setValue(getContextProperty(), value);
                } catch (CloneNotSupportedException e) {
                    throw new OpenlNotCheckedException(e);
                }
            } else {
                rulesRuntimeContext.setValue(getContextProperty(), value);
            }
        }
        return rulesRuntimeContext;
    }

    protected abstract Object getValue(Object[] params, IRuntimeEnv env);

    protected abstract boolean isProcessable(Object[] params);

    protected abstract String getContextProperty();

}
