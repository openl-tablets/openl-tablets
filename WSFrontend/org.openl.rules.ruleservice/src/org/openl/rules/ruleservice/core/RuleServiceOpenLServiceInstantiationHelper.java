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

    public static IOpenMember extractOpenMember(Method method, Object serviceTarget) {
        IOpenMember openMember = null;
        for (Class<?> cls : serviceTarget.getClass().getInterfaces()) {
            try {
                Method m = cls.getMethod(method.getName(), method.getParameterTypes());
                openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget);
                if (openMember != null) {
                    break;
                }
            } catch (NoSuchMethodException ignore) {
            }
        }
        return openMember;
    }
}
