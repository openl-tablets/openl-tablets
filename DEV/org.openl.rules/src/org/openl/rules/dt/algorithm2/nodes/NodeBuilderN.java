package org.openl.rules.dt.algorithm2.nodes;

import java.util.Iterator;

import org.openl.rules.dt.algorithm2.ISearchTreeNode;
import org.openl.rules.dt.algorithm2.NodeBuilder;
import org.openl.rules.dt.element.ICondition;

public abstract class NodeBuilderN extends NodeBuilder {

	public NodeBuilderN(ICondition condition, boolean isFirst, boolean isLast) {
		super(condition, isFirst, isLast);
	}

	@Override
	public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
		throw new UnsupportedOperationException("indexRuleN");
	}

	@Override
	public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node, int ruleN) {

		int index = calculateNodeIndex(ruleN);
		SearchNodeN snode = (SearchNodeN) node;
		IARTNode nextNode = snode.findNode(index);
		if (nextNode == null) {
			snode.setNode(index, nextNode = next.createNode());
		}

		return (ISearchTreeNode) nextNode;
	}

	@Override
	public ISearchTreeNode createNode() {
		return new SearchNodeN(new IARTNode[getNodesSize()]);
	}

	abstract protected int getNodesSize();

	abstract protected int calculateNodeIndex(int ruleN);

	// protected int calculateNodeIndex(int ruleN) {
	// Object paramValue = condition.getParamValue(0, ruleN);
	// int index = map.get(paramValue);
	//
	// return index;
	// }

	static abstract public class SingleN extends NodeBuilderN {

		public SingleN(ICondition condition, boolean isFirst,
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