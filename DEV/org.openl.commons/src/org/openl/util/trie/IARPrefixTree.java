package org.openl.util.trie;

import java.util.Iterator;

public interface IARPrefixTree<V> extends IARTree<V> {
	
	/**
	 * Returns {@link Iterator}<V> that starts from the longest prefix in the key chain and goes to the shortest
	 * For example
	 * 
	 * tree.put("abcd", 1);
	 * tree.put("ab", 2);
	 * tree.put("abc", 3);
	 * Ieterator it = tree.startsWith("abcde");
	 * 
	 * for(;;)
	 * 
	 * 
	 * @param key
	 * @return 
	 */
	Iterator<V> allPrefixesOf(ISequentialKey key);
	Iterator<V> allPrefixesOf(CharSequence key);
	
	V getLongestPrefixValue(ISequentialKey key);
	V getLongestPrefixValue(CharSequence key);
	
}
