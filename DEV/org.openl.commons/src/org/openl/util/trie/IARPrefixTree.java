package org.openl.util.trie;

import java.util.Iterator;

public interface IARPrefixTree<K extends ISequentialKey, V> extends IARTree<K, V> {
	
	Iterator<V> startsWith(K key);
}
