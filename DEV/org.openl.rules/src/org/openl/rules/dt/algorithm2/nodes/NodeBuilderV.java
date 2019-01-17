package org.openl.rules.dt.algorithm2.nodes;

import java.util.Iterator;

import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.rules.dt.algorithm2.NodeBuilder;
import org.openl.rules.dt.element.ICondition;

public abstract class NodeBuilderV extends NodeBuilder {

	public NodeBuilderV(ICondition condition, boolean isFirst, boolean isLast) {
		super(condition, isFirst, isLast);
	}

	@Override
	public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
		int index = calculateNodeIndex(ruleN);
		SearchNodeV vnode = (SearchNodeV) node;
		
		Object oldValue =  vnode.getValue(index);
		
		if (oldValue == null) {
			vnode.setValue(index, ruleN);
			return true;
		}

		return false;
	}
	
	

	@Override
	public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node, int ruleN) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISearchTreeNode createNode() {
		return new SearchNodeV(new int[getNodesSize()]);
	}

	abstract protected int getNodesSize();

	abstract protected int calculateNodeIndex(int ruleN);

	static abstract public class SingleV extends NodeBuilderV {

		public SingleV(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
		}

		@Override
		public boolean isSingleNode(int ruleN) {
			return true;
		}

		@Override
		public Iterator<ISearchTreeNode> findOrCreateNextNodes(
				ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException("findOrCreateNextNodes");
		}


	}

}