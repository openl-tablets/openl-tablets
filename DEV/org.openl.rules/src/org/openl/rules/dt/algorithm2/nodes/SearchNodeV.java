package org.openl.rules.dt.algorithm2.nodes;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;

public class SearchNodeV implements ISearchTreeNode{

	SearchNodeV(int[] values) {
		this.values = values;
	}

	private static final int MAGIC_VALUE = 0xffffffff;

	int[] values;

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

	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {
		Object res = scxt.getIndexedValue();
		
		return res == null ? null : getValue((Integer) res);
	}

	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		return null;
	}

	@Override
	public ISearchTreeNode compactSearchNode() {
		return this;
	}
}
