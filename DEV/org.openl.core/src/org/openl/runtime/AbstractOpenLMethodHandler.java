package org.openl.runtime;

import org.openl.types.IOpenMember;

public abstract class AbstractOpenLMethodHandler<K, V> implements IOpenLMethodHandler<K, V> {
    @Override
    @SuppressWarnings("unchecked")
    public IOpenMember getOpenMember(K key) {
        ASMProxyHandler proxyHandler = ASMProxyFactory.getProxyHandler(getTarget());
        if (proxyHandler instanceof IOpenLMethodHandler) {
            return ((IOpenLMethodHandler<V, ?>) proxyHandler).getOpenMember(getTargetMember(key));
        }
        throw new IllegalStateException("proxyHandler is not an instance of class OpenLMethodHandler");
    }
}
