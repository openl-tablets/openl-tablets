package org.openl.rules.dt2.algorithm2.nodes;

import java.util.Iterator;

import org.openl.rules.dt2.algorithm2.ConditionDescriptor;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.NodeBuilder;
import org.openl.rules.dt2.element.ICondition;

public class EmptyNodeBuilder {

	public static NodeBuilder makeBuilder(ICondition condition, boolean isFirst,
			boolean isLast) {
		return isLast ? new EmptyBuilderI(condition, isFirst, isLast) : new EmptyBuilderN(condition, isFirst, isLast);
	}
	
	
	static class EmptyBuilderI extends NodeBuilder
	{

		public EmptyBuilderI(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			((EmptySearchNodeRi)node).addRule(ruleN);
			return true;
		}

		@Override
		public boolean isSingleNode(int ruleN) {
			return false;
		}

		@Override
		public Iterator<ISearchTreeNode> findOrCreateNextNodes(
				ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ISearchTreeNode createNode() {
			return new EmptySearchNodeRi();
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static class EmptyBuilderN extends NodeBuilder.Single {

		

		public EmptyBuilderN(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException();
		}

		
		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			EmptySearchNodeRN.SingleRN rn = (EmptySearchNodeRN.SingleRN)node;
			
			
			return rn.node;
		}


		@Override
		public ISearchTreeNode createNode() {
			return new EmptySearchNodeRN.SingleRN(next.createNode());
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			throw new UnsupportedOperationException();
		}
	}
	
	

}
