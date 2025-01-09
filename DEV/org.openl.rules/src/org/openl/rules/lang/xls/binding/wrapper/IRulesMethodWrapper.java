package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.vm.SimpleRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public interface IRulesMethodWrapper extends IOpenMethodWrapper {
    default Object invokeDelegateWithContextPropertiesInjector(Object target,
                                                               Object[] params,
                                                               IRuntimeEnv env,
                                                               SimpleRuntimeEnv simpleRuntimeEnv) {
        boolean isNewRuntimeContext = getContextPropertiesInjector().push(params, env, simpleRuntimeEnv);
        IRulesMethodWrapper rulesMethodWrapper = simpleRuntimeEnv.getMethodWrapper();
        try {
            simpleRuntimeEnv.setMethodWrapper(this);
            return getDelegate().invoke(target, params, env);
        } finally {
            simpleRuntimeEnv.setMethodWrapper(rulesMethodWrapper);
            if (isNewRuntimeContext) {
                getContextPropertiesInjector().pop(simpleRuntimeEnv);
            }
        }
    }

    ContextPropertiesInjector getContextPropertiesInjector();

    XlsModuleOpenClass getXlsModuleOpenClass();

    IOpenMethod getTopOpenClassMethod(IOpenClass openClass);

    IOpenClass getType();
}
