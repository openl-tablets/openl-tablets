package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeVi;
import org.openl.util.trie.nodes.IntArrayIterator;

public class ARTNode0Vi extends IARTNodeN.EmptyARTNodeN implements IARTNode, IARTNodeVi {


	private static final int MAGIC_VALUE = 0xffffffff;
	

	int[] values;

	private int count;


	public ARTNode0Vi(int count, int[] values) {
		this.count = count;
		this.values = values;
	}


	@Override
	public Object getValue(int index) {
//		if (index < 0 || index >= values.length)
//			return null;
		return values[index] == 0 ? null : MAGIC_VALUE - values[index];
	}





	@Override
	public void setValue(int index, Object value) {
		values[index] = MAGIC_VALUE - (Integer)value;

	}

	@Override
	public int countV() {
		return count;
	}

	@Override
	public int minIndexV() {
		return 0;
	}

	@Override
	public int maxIndexV() {
		return values.length - 1;
	}


	@Override
	public IIntIterator indexIteratorV() {

		return IntArrayIterator.iterator(0, values);
	}


	@Override
	public IARTNode compact() {
		return this;
	}

	
	
}
