package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public interface IRulesMethodWrapper extends IOpenMethodWrapper {
    default Object invokeDelegateWithContextPropertiesInjector(Object target,
            Object[] params,
            IRuntimeEnv env,
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv) {
        boolean isNewRuntimeContext = getContextPropertiesInjector().push(params, env, simpleRulesRuntimeEnv);
        try {
            return getDelegate().invoke(target, params, env);
        } finally {
            if (isNewRuntimeContext) {
                getContextPropertiesInjector().pop(simpleRulesRuntimeEnv);
            }
        }
    }

    ContextPropertiesInjector getContextPropertiesInjector();

    XlsModuleOpenClass getXlsModuleOpenClass();

    IOpenMethod getTopOpenClassMethod(IOpenClass openClass);

}
