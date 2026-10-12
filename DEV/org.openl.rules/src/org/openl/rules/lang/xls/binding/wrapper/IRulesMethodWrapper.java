package org.openl.rules.lang.xls.binding.wrapper;

import java.util.function.Supplier;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;

public interface IRulesMethodWrapper extends IOpenMethodWrapper {
    default Object invokeDelegateWithContextPropertiesInjector(Object target,
                                                               Object[] params,
                                                               IRuntimeEnv env,
                                                               SimpleRuntimeEnv simpleRuntimeEnv) {
        return withInjectedContext(params, env, simpleRuntimeEnv, () -> {
            var rulesMethodWrapper = simpleRuntimeEnv.getMethodWrapper();
            try {
                simpleRuntimeEnv.setMethodWrapper(this);
                return getDelegate().invoke(target, params, env);
            } finally {
                simpleRuntimeEnv.setMethodWrapper(rulesMethodWrapper);
            }
        });
    }

    /**
     * Runs an action with the runtime context completed by the context properties taken from the arguments.
     * The context is restored when the action ends.
     */
    default <T> T withInjectedContext(Object[] params,
                                      IRuntimeEnv env,
                                      SimpleRuntimeEnv simpleRuntimeEnv,
                                      Supplier<T> action) {
        var isNewRuntimeContext = getContextPropertiesInjector().push(params, env, simpleRuntimeEnv);
        try {
            return action.get();
        } finally {
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
