package org.openl.util.trie.cnodes;

import org.openl.util.trie.IARTNode;

public class ARTNode0Vi implements IARTNode {

	private static final int MAGIC_VALUE = 0xffffffff;

	int[] values;

	public ARTNode0Vi(int[] values) {
		this.values = values;
	}

	@Override
	public Object getValue(int index) {
		return values[index] == 0 ? null : MAGIC_VALUE - values[index];
	}

	@Override
	public void setValue(int index, Object value) {
		values[index] = MAGIC_VALUE - (Integer)value;
	}

	@Override
	public void setNode(int index, IARTNode node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IARTNode findNode(int index) {
		return null;
	}
}
