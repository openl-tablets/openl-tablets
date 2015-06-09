package org.openl.util.trie;

import java.util.Iterator;

public interface IARTreeBase<V> {
	
	void put(ISequentialKey key, V value);
	V get(ISequentialKey key);
	
	Iterator<IARTNode> nodeIteratorDepthFirst();
	void compact();
	

}
