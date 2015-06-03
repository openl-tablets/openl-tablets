package org.openl.util.trie;

public interface IARTree<K extends ISequentialKey,V> {
	
	void put(K key, V value);
	V get(K key);

}
