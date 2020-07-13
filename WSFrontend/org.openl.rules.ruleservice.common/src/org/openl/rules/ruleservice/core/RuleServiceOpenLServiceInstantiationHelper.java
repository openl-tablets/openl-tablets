package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;

import org.openl.runtime.ASMProxyFactory;
import org.openl.runtime.ASMProxyHandler;
import org.openl.runtime.IOpenLMethodHandler;
import org.openl.types.IOpenMember;

public final class RuleServiceOpenLServiceInstantiationHelper {

    private RuleServiceOpenLServiceInstantiationHelper() {
        // Hidden constructor
    }

    @SuppressWarnings("unchecked")
    public static IOpenMember getOpenMember(Method method, Object serviceTarget) {
        if (ASMProxyFactory.isProxy(serviceTarget)) {
            ASMProxyHandler proxyHandler = ASMProxyFactory.getProxyHandler(serviceTarget);
            if (proxyHandler instanceof IOpenLMethodHandler) {
                return ((IOpenLMethodHandler<Method, ?>) proxyHandler).getOpenMember(method);
            }
        }
        return null;
    }
}
