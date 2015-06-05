package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.nodes.ObjectArrayIterator;

public class ARTNode1V extends IARTNodeN.EmptyARTNodeN implements IARTNode {


	int start;

	Object[] values;

	private int count;


	public ARTNode1V(int start, int count, Object[] values) {
		this.start = start;
		this.count = count;
		this.values = values;
	}


	@Override
	public Object getValue(int index) {
		int idx = index - start;
		if (idx < 0 || idx >= values.length)
			return null;
		return values[idx];
	}


	@Override
	public void setValue(int index, Object value) {

		values[index - start] = value;

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
		return ObjectArrayIterator.iterator(start, values);
	}


	@Override
	public IARTNode compact() {
		return this;
	}

}
