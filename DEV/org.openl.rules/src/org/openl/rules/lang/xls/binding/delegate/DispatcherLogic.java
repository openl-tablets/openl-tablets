package org.openl.rules.lang.xls.binding.delegate;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class DispatcherLogic {
    public static ThreadLocal<IOpenClass> topClassRef = new ThreadLocal<IOpenClass>();
    public static ThreadLocal<Boolean> isInvokedFromTop = new BooleanThreadLocal();

    private static class BooleanThreadLocal extends ThreadLocal<Boolean> {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    }

    public static Object dispatch(XlsModuleOpenClass xlsModuleOpenClass, IOpenMethod delegate, Object target, Object[] params, IRuntimeEnv env) {
        IOpenClass topClass = topClassRef.get();
        if (topClass == null) {
            try {
                IOpenClass typeClass;
                if (target instanceof IDynamicObject) {
                    IDynamicObject dynamicObject = (IDynamicObject) target;
                    typeClass = dynamicObject.getType();
                } else if (java.lang.reflect.Proxy.isProxyClass(target.getClass())) {
                    java.lang.reflect.InvocationHandler invocationHandler = java.lang.reflect.Proxy.getInvocationHandler(target);
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
                topClassRef.set(typeClass);
                return delegate.invoke(target, params, env);
            } finally {
                topClassRef.remove();
                isInvokedFromTop.remove();
            }
        } else {
            Boolean f = isInvokedFromTop.get();
            if (topClass != xlsModuleOpenClass) {
                if (!Boolean.TRUE.equals(f)) {
                    IOpenMethod matchedMethod = topClass.getMatchingMethod(delegate.getName(), delegate.getSignature()
                        .getParameterTypes());
                    if (matchedMethod != null) {
                        isInvokedFromTop.set(Boolean.TRUE);
                        return matchedMethod.invoke(target, params, env);
                    }
                } else {
                    if (!(delegate instanceof MatchingOpenMethodDispatcher || delegate instanceof OverloadedMethodsDispatcherTable)) {
                        isInvokedFromTop.remove();
                    }
                }
            }
        }
        return delegate.invoke(target, params, env);
    }
}
