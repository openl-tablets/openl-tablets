package org.openl.rules.dt2.algorithm2.nodes;

import java.util.Map;

import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;

public class EqualsSearchNodeMap extends BaseSearchNode {
	
	
	Map<Object, Object> map ; 

	/**
	 * EqualsSearchNode always produces a single return 
	 */
	
	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		return null;
	}

	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {
		return map.get(scxt.getIndexedValue());
	}

	@Override
	public ISearchTreeNode compactSearchNode() {
		for (Map.Entry<Object, Object> e : map.entrySet()) {
			Object v = e.getValue();
			if (v instanceof ISearchTreeNode) {
				e.setValue( ((ISearchTreeNode) v).compactSearchNode());
				
			}
		}
		return this;
	}


}
