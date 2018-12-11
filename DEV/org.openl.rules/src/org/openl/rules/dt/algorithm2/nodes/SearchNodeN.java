package org.openl.rules.dt.algorithm2.nodes;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.util.trie.IARTNode;

public class SearchNodeN implements ISearchTreeNode{

	private IARTNode[] nodes;

	SearchNodeN(IARTNode[] nodes) {
		this.nodes = nodes;
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
		if (nodes != null) {
			for (int idx = 0; idx < nodes.length; idx++) {
				ISearchTreeNode oldNode = (ISearchTreeNode) findNode(idx);
				if (oldNode != null) {
					setNode(idx, oldNode.compactSearchNode());
				}
			}
		}
		return this;
	}

	@Override
	public IARTNode findNode(int index) {
		return nodes[index];
	}

	@Override
	public void setNode(int index, IARTNode node) {
		nodes[index] = node;
	}

	@Override
	public void setValue(int index, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getValue(int index) {
		return null;
	}
}
