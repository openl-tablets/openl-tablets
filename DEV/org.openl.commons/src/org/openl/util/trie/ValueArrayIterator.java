package org.openl.util.trie;

import org.openl.util.AOpenIterator;

public class ValueArrayIterator<V> extends AOpenIterator<V> {

	Object[] ary; 
	int maxIndex;
	int current;

	public ValueArrayIterator(Object[] ary, int maxIndex) {
		super();
		this.ary = ary;
		this.maxIndex = maxIndex;
		this.current = maxIndex;
	}

	

	@Override
	public boolean hasNext() {
		return current >= 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V next() {
		
		V ret = (V)ary[current];
		while(--current >= 0 && ary[current] == null);
		return ret;
	}

}
