package org.openl.rules.lang.xls.binding.wrapper;

import java.lang.reflect.Array;

import org.openl.domain.IDomain;
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
    private static void validateForAliasDatatypeParameter(IOpenClass parameterType, Object value) {
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object v = Array.get(value, i);
                validateForAliasDatatypeParameter(parameterType, v);
            }
            return;
        }
        @SuppressWarnings("rawtypes")
        IDomain domain = parameterType.getDomain();
        if (!domain.selectObject(value)) {
            throw new InputParameterOutsideOfValidDomainException(
                String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                    value,
                    parameterType.getName(),
                    DomainUtils.toString(domain)));
        }
    }
    
    private static void validateParameters(IOpenMethod method, Object[] params) {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    IOpenClass parameterType = method.getSignature().getParameterType(i);
                    if (parameterType.getDomain() != null) {
                        validateForAliasDatatypeParameter(parameterType, params[i]);
                    }
                }
            }
        }
    }
    
    public static IOpenMethod getTopClassMethod(IOpenMethodWrapper wrapper, IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
        if (topClass != null && topClass != wrapper.getXlsModuleOpenClass()) {
            IOpenMethod method = TopClassMethodCache.getInstance().getTopClassMethod(topClass, wrapper);
            if (method != null) {
                method = extractMethod(simpleRulesRuntimeEnv, method);
                if (method != wrapper) {
                    return method;
                }
            }
        }
        return wrapper.getDelegate();
    }

    private static SimpleRulesRuntimeEnv extractSimpleRulesRuntimeEnv(IRuntimeEnv env) {
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
        return simpleRulesRuntimeEnv;
    }

    private static IOpenMethod extractMethod(SimpleRulesRuntimeEnv simpleRulesRuntimeEnv,
            IOpenMethod method) {
        while (method instanceof LazyMethodWrapper || method instanceof MethodDelegator) {
            if (method instanceof LazyMethodWrapper) {
                method = ((LazyMethodWrapper) method)
                    .getCompiledMethod(simpleRulesRuntimeEnv);
            }
            if (method instanceof MethodDelegator) {
                MethodDelegator methodDelegator = (MethodDelegator) method;
                method = methodDelegator.getMethod();
            }
        }
        return method;
    }
    
    public static Object invoke(IOpenMethodWrapper wrapper,
            Object target,
            Object[] params,
            IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
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
                Thread.currentThread().setContextClassLoader(wrapper.getXlsModuleOpenClass().getClassLoader());
                validateParameters(wrapper.getDelegate(), params);
                return wrapper.getDelegate().invoke(target, params, env);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
                simpleRulesRuntimeEnv.setTopClass(null);
            }
        } else {
            if (topClass != wrapper.getXlsModuleOpenClass()) {
                IOpenMethod method = TopClassMethodCache.getInstance().getTopClassMethod(topClass, wrapper);
                if (method != null) {
                    method = extractMethod(simpleRulesRuntimeEnv, method);
                    if (method != wrapper) {
                        return method.invoke(target, params, env);
                    }
                }
            }
        }
        return wrapper.getDelegate().invoke(target, params, env);
    }
}
