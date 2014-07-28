package org.openl.rules.ui.tree;

import java.util.Collection;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public class TreeCache<K, V> {

    private BidiMap<K, V> nodesMap = new DualHashBidiMap<K, V>();

    public TreeCache() {
    }

    @SuppressWarnings("unchecked")
    public V getNode(K key) {
        return nodesMap.get(key);
    }

    public Collection<V> getAllNodes() {
        return nodesMap.values();
    }

    @SuppressWarnings("unchecked")
    public K getKey(V node) {
        return nodesMap.getKey(node);
    }

    public void put(K key, V node) {
        nodesMap.put(key, node);
    }

    public void remove(K key) {
        nodesMap.remove(key);
    }

    public void clear() {
        nodesMap.clear();
    }

}
