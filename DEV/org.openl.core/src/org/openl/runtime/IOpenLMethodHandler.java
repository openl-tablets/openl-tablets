package org.openl.runtime;

import javassist.util.proxy.MethodHandler;

public interface IOpenLMethodHandler<K, V> extends MethodHandler {

    Object getTarget();

    V getTargetMember(K key);

}
