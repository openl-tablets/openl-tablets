package org.openl.runtime;

import java.lang.reflect.InvocationHandler;

public interface IOpenLInvocationHandler<K, V> extends InvocationHandler {
    
    Object getTarget();
    
    V getTargetMember(K key);
    
}
