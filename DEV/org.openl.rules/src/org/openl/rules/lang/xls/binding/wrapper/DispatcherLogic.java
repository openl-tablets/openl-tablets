package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public final class DispatcherLogic {

    private DispatcherLogic() {
    }
    

    public static Object dispatch(XlsModuleOpenClass xlsModuleOpenClass, IOpenMethod delegate, Object target, Object[] params, IRuntimeEnv env) {
        IRuntimeEnv env1 = env;
        if (env instanceof TBasicContextHolderEnv){
            TBasicContextHolderEnv tBasicContextHolderEnv = (TBasicContextHolderEnv) env;
            env1 = tBasicContextHolderEnv.getEnv();
            while (env1 instanceof TBasicContextHolderEnv){
                tBasicContextHolderEnv = (TBasicContextHolderEnv) env1;
                env1 = tBasicContextHolderEnv.getEnv();
            }
        }
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env1;
        
        IOpenClass topClass = simpleRulesRuntimeEnv.getTopClass();
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
                simpleRulesRuntimeEnv.setTopClass(typeClass);
                return delegate.invoke(target, params, env);
            } finally {
                simpleRulesRuntimeEnv.setTopClass(null);
                simpleRulesRuntimeEnv.setInvokedFromTop(false);
            }
        } else {
            boolean f = simpleRulesRuntimeEnv.isInvokedFromTop();
            if (topClass != xlsModuleOpenClass) {
                if (!f) {
                    IOpenMethod matchedMethod = topClass.getMatchingMethod(delegate.getName(), delegate.getSignature()
                        .getParameterTypes());
                    if (matchedMethod != null) {
                        simpleRulesRuntimeEnv.setInvokedFromTop(true);
                        return matchedMethod.invoke(target, params, env);
                    }
                } else {
                    //if (!(delegate instanceof MatchingOpenMethodDispatcher || delegate instanceof OverloadedMethodsDispatcherTable)) {
                    simpleRulesRuntimeEnv.setInvokedFromTop(false);
                    //}
                }
            }
        }
        return delegate.invoke(target, params, env);
    }
}
