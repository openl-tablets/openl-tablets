package org.openl.rules.ruleservice.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.openl.runtime.IOpenLInvocationHandler;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.types.IOpenMember;

public final class RuleServiceOpenLServiceInstantiationHelper {

    private RuleServiceOpenLServiceInstantiationHelper() {
        // Hidden constructor
    }

    public static final IOpenMember getOpenMember(Method method, Object serviceTarget) {
        Object t = serviceTarget;
        Object key = method;
        while (Proxy.isProxyClass(t.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(t);
            if (invocationHandler instanceof IOpenLInvocationHandler) {
                @SuppressWarnings("unchecked")
                IOpenLInvocationHandler<Object, Object> openLInvocationHandler = (IOpenLInvocationHandler<Object, Object>) invocationHandler;
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
