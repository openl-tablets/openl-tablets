package org.openl.util.trie;

import java.util.Iterator;

public interface IARSuffixTree<K extends ISequentialKey, V> extends IARTree<K, V> {
	
	Iterator<V> endsWith(K key);
}
