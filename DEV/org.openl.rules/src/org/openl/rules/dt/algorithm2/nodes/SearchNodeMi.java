package org.openl.rules.dt.algorithm2.nodes;

import java.util.Map;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;

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
