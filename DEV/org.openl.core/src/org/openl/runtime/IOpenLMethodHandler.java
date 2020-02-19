package org.openl.runtime;

public interface IOpenLMethodHandler<K, V> extends OpenLProxyHandler {

    Object getTarget();

    V getTargetMember(K key);

}
