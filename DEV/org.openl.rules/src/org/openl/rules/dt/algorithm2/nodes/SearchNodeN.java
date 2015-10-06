package org.openl.rules.dt.algorithm2.nodes;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.cnodes.ARTNode0N;

public class SearchNodeN extends ARTNode0N implements ISearchTreeNode{

	public SearchNodeN(IARTNode[] nodes) {
		super(0, nodes);
	}

	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {
		Object res = scxt.getIndexedValue();
		if (res == null)
			return null;
		return findNode((Integer)res);
	}

	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		return null;
	}

	@Override
	public ISearchTreeNode compactSearchNode() {
		 IIntIterator indexIteratorN = indexIteratorN();
		 
		while(indexIteratorN.hasNext())
		{
			int idx = indexIteratorN.nextInt();
			
			ISearchTreeNode oldNode = (ISearchTreeNode) findNode(idx);
			setNode(idx, oldNode.compactSearchNode());
		}	
		return this;
	}

}
