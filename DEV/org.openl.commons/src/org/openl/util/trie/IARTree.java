package org.openl.util.trie;


/**
 * Main interface for efficient RadixTree implementation. The tree support keys that represent a sequence of integer values.
 * The tree supports CharSequence keys for key values from 0 to 0xffff and  ISequenceKey interface for key values from 0 to 0xffffffff
 * 
 * 
 * @author snshor
 *
 */

public interface IARTree<V> extends IARTreeBase<V> {
	
	
	void put(CharSequence key, V value);
	
	V get(CharSequence key);

}
