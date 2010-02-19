package org.openl.rules.ui.tree;

import java.util.Collection;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

public class TreeCache<K, V> {

    private BidiMap nodesMap = new DualHashBidiMap();

    public TreeCache() {
    }

    @SuppressWarnings("unchecked")
    public V getNode(K key) {
        return (V) nodesMap.get(key);
    }

    public Collection<?> getAllNodes() {
        return nodesMap.values();
    }

    @SuppressWarnings("unchecked")
    public K getKey(V node) {
        return (K) nodesMap.getKey(node);
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
