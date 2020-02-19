package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;

import org.openl.runtime.IOpenLMethodHandler;
import org.openl.runtime.OpenLASMProxy;
import org.openl.runtime.OpenLProxyHandler;
import org.openl.types.IOpenMember;

public final class RuleServiceOpenLServiceInstantiationHelper {

    private RuleServiceOpenLServiceInstantiationHelper() {
        // Hidden constructor
    }

    public static final IOpenMember getOpenMember(Method method, Object serviceTarget) {
        Object t = serviceTarget;
        Object key = method;
        while (OpenLASMProxy.isProxy(t)) {
            OpenLProxyHandler invocationHandler = OpenLASMProxy.getHandler(t);
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
