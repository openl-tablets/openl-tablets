package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.IARTNodeVI;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.nodes.IntArrayIterator;

public class ARTNode1VI implements IARTNodeVI {


	private static final int MAGIC_VALUE = 0xffffffff;
	
	int start;

	int[] values;

	private int count;


	public ARTNode1VI(int start, int count, int[] values) {
		this.start = start;
		this.count = count;
		this.values = values;
	}


	@Override
	public Object getValue(int index) {
		int idx = index - start;
		if (idx < 0 || idx >= values.length)
			return null;
		return values[idx] == 0 ? null : MAGIC_VALUE - values[idx];
	}





	@Override
	public IARTNodeV setValue(int index, Object value) {
		values[index - start] = MAGIC_VALUE - (Integer)value;

		return this;
	}

	@Override
	public int countV() {
		return count;
	}

	@Override
	public int minIndexV() {
		return start;
	}

	@Override
	public int maxIndexV() {
		return start + values.length - 1;
	}


	@Override
	public IIntIterator indexIteratorV() {

		return IntArrayIterator.iterator(start, values);
	}


	@Override
	public IARTNodeX compact() {
		return this;
	}

	
	
}
