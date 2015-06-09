package org.openl.util.trie;

import java.util.Iterator;

public interface IARSuffixTree<V> extends IARTree<V> {
	

	Iterator<V> allSuffixesOf(ISequentialKey key);
	Iterator<V> allSuffixesOf(CharSequence key);
	
	V getLongestSuffixValue(ISequentialKey key);
	V getLongestSuffixValue(CharSequence key);

}
