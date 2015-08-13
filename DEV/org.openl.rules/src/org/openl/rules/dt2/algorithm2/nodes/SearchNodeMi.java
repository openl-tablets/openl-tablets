package org.openl.rules.dt2.algorithm2.nodes;

import java.util.Map;

import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.util.trie.IARTNode;

public class SearchNodeMi extends IARTNode.EmptyARTNode implements ISearchTreeNode {

	public SearchNodeMi(Map<Object, Integer> map) {
		super();
		this.map = map;
	}

	Map<Object, Integer> map;
	
	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {
		
		return map.get(scxt.getIndexedValue());
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
