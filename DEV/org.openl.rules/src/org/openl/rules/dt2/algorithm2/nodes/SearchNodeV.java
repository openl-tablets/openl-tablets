package org.openl.rules.dt2.algorithm2.nodes;

import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.util.trie.cnodes.ARTNode0Vi;

public class SearchNodeV extends ARTNode0Vi implements ISearchTreeNode{

	public SearchNodeV(int[] values) {
		super(0, values);
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
