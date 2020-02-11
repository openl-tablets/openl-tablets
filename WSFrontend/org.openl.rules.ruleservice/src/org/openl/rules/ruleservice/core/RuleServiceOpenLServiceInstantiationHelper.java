package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;

import org.openl.runtime.IOpenLMethodHandler;
import org.openl.types.IOpenMember;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

public final class RuleServiceOpenLServiceInstantiationHelper {

    private RuleServiceOpenLServiceInstantiationHelper() {
        // Hidden constructor
    }

    public static final IOpenMember getOpenMember(Method method, Object serviceTarget) {
        Object t = serviceTarget;
        Object key = method;
        while (t instanceof ProxyObject) {
            MethodHandler invocationHandler = ((ProxyObject) t).getHandler();
            if (invocationHandler instanceof IOpenLMethodHandler) {
                @SuppressWarnings("unchecked")
                IOpenLMethodHandler<Object, Object> openLInvocationHandler = (IOpenLMethodHandler<Object, Object>) invocationHandler;
                t = openLInvocationHandler.getTarget();
                key = openLInvocationHandler.getTargetMember(key);
                if (key instanceof IOpenMember) {
                    return (IOpenMember) key;
                }
            } else {
                return null;
            }
        }
        return null;
    }
}
