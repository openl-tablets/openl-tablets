package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.prebind.LazyMethodWrapper;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodDelegator;
import org.openl.util.DomainUtils;
import org.openl.vm.IRuntimeEnv;

public final class WrapperLogic {

    private WrapperLogic() {
    }

    @SuppressWarnings("unchecked")
    private static void validateParameters(IOpenMethod method, Object[] params) {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                IOpenClass parameterType = method.getSignature().getParameterType(i);
                if (parameterType.getDomain() != null) {
                    @SuppressWarnings("rawtypes")
                    IDomain domain = parameterType.getDomain();
                    if (!domain.selectObject(params[i])) {
                        throw new OutsideOfValidDomainException(
                            String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                                params[i],
                                parameterType.getName(),
                                DomainUtils.toString(domain)));
                    }
                }
            }
        }
    }

    public static Object invoke(XlsModuleOpenClass xlsModuleOpenClass,
            IOpenMethodWrapper wrapper,
            Object target,
            Object[] params,
            IRuntimeEnv env) {
        IRuntimeEnv env1 = env;
        if (env instanceof TBasicContextHolderEnv) {
            TBasicContextHolderEnv tBasicContextHolderEnv = (TBasicContextHolderEnv) env;
            env1 = tBasicContextHolderEnv.getEnv();
            while (env1 instanceof TBasicContextHolderEnv) {
                tBasicContextHolderEnv = (TBasicContextHolderEnv) env1;
                env1 = tBasicContextHolderEnv.getEnv();
            }
        }
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env1;

        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass == null) {
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                IOpenClass typeClass;
                if (target instanceof IDynamicObject) {
                    IDynamicObject dynamicObject = (IDynamicObject) target;
                    typeClass = dynamicObject.getType();
                } else if (java.lang.reflect.Proxy.isProxyClass(target.getClass())) {
                    java.lang.reflect.InvocationHandler invocationHandler = java.lang.reflect.Proxy
                        .getInvocationHandler(target);
                    if (invocationHandler instanceof OpenLInvocationHandler) {
                        OpenLInvocationHandler openLInvocationHandler = (OpenLInvocationHandler) invocationHandler;
                        Object openlInstance = openLInvocationHandler.getInstance();
                        if (openlInstance instanceof IDynamicObject) {
                            IDynamicObject dynamicObject = (IDynamicObject) openlInstance;
                            typeClass = dynamicObject.getType();
                        } else {
                            throw new IllegalStateException("Can't define openl class from target object!");
                        }
                    } else {
                        throw new IllegalStateException("Can't define openl class from target object!");
                    }
                } else {
                    throw new IllegalStateException("Can't define openl class from target object");
                }
                simpleRulesRuntimeEnv.setTopClass(typeClass);
                Thread.currentThread().setContextClassLoader(xlsModuleOpenClass.getClassLoader());
                validateParameters(wrapper.getDelegate(), params);
                return wrapper.getDelegate().invoke(target, params, env);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
                simpleRulesRuntimeEnv.setTopClass(null);
            }
        } else {
            if (topClass != xlsModuleOpenClass) {
                IOpenMethod matchedMethod = simpleRulesRuntimeEnv.getTopClassMethod(wrapper);
                if (matchedMethod != null) {
                    while (matchedMethod instanceof LazyMethodWrapper || matchedMethod instanceof MethodDelegator) {
                        if (matchedMethod instanceof LazyMethodWrapper) {
                            matchedMethod = ((LazyMethodWrapper) matchedMethod)
                                .getCompiledMethod(simpleRulesRuntimeEnv);
                        }
                        if (matchedMethod instanceof MethodDelegator) {
                            MethodDelegator methodDelegator = (MethodDelegator) matchedMethod;
                            matchedMethod = methodDelegator.getMethod();
                        }
                    }
                    if (matchedMethod != wrapper) {
                        return matchedMethod.invoke(target, params, env);
                    }
                }
            }
        }
        return wrapper.getDelegate().invoke(target, params, env);
    }
}
