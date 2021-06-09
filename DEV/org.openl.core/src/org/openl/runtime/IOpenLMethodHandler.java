package org.openl.runtime;

import org.openl.types.IOpenMember;

public interface IOpenLMethodHandler<K, V> extends ASMProxyHandler {

    Object getTarget();

    V getTargetMember(K key);

    IOpenMember getOpenMember(K key);
}
