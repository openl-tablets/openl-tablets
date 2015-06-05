package org.openl.util.trie;

import java.util.Iterator;

public interface IARTree<K extends ISequentialKey,V> {
	
	void put(K key, V value);
	V get(K key);
	
	Iterator<IARTNode> nodeIteratorDepthFirst();
	void compact();

}
