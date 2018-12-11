package org.openl.rules.dt.algorithm2.nodes;

import java.util.Map;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;

public class SearchNodeMN extends IARTNode.EmptyARTNode implements ISearchTreeNode {

	public SearchNodeMN(Map<Object, ISearchTreeNode> map) {
		super();
		this.map = map;
	}

	Map<Object, ISearchTreeNode> map;
	
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
		for (Map.Entry<Object, ISearchTreeNode> e : map.entrySet()) {
			e.setValue(e.getValue().compactSearchNode());
			
		}
		return this;
	}


}
