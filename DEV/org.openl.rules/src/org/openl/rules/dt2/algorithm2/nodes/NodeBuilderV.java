package org.openl.rules.dt2.algorithm2.nodes;

import java.util.Iterator;

import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.NodeBuilder;
import org.openl.rules.dt2.element.ICondition;

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

	// protected int calculateNodeIndex(int ruleN) {
	// Object paramValue = condition.getParamValue(0, ruleN);
	// int index = map.get(paramValue);
	//
	// return index;
	// }

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