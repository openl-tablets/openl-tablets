package org.openl.rules.dt2.algorithm2.nodes;

import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dt2.algorithm2.ConditionDescriptor;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.NodeBuilder;
import org.openl.rules.dt2.element.ICondition;

public class DefaultNodeBuilder {

	public static NodeBuilder makeNodeBuilder(ICondition cond, boolean isFirst,
			boolean isLast, IndexInfo info) {
		if (isFirst)
			if (isLast)
				return new DefaultNodeBuilderAi(cond, isFirst, isLast, info);
			else
				return new DefaultNodeBuilderRN(cond, isFirst, isLast, info);
		else if (isLast)
			return new DefaultNodeBuiderRi(cond, isFirst, isLast);
		else
			return new DefaultNodeBuilderRN(cond, isFirst, isLast, info);
	}

	public static class DefaultNodeBuilderAi extends NodeBuilder.Single {

		private IndexInfo info;

		public DefaultNodeBuilderAi(ICondition condition, boolean isFirst,
				boolean isLast, IndexInfo info) {
			super(condition, isFirst, isLast);
			this.info = info;
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			return true;
		}

		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			throw new UnsupportedOperationException();
		}


		@Override
		public ISearchTreeNode createNode() {
			return new DefaultSearchNodeAi(info.getFromRule(), info.getToRule(), info.getStep());
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor(false, condition);
		}
	}
	
	
	public static class DefaultNodeBuilderRN extends NodeBuilder.Single {

		private IndexInfo info;
		
		public DefaultNodeBuilderRN(ICondition condition, boolean isFirst,
				boolean isLast, IndexInfo info) {
			super(condition, isFirst, isLast);
			this.info = info;
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			throw new UnsupportedOperationException();
		}

		
		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			DefaultSearchNodeRN rn = (DefaultSearchNodeRN)node;
			
			ISearchTreeNode nextNode = rn.useLastNode(ruleN, info, condition);
			
			if (nextNode != null)
			{
				return nextNode;
			}
			
			nextNode = next.createNode();
			
			rn.addNewNode(ruleN, nextNode);
			
			return nextNode;
		}


		@Override
		public ISearchTreeNode createNode() {
			return new DefaultSearchNodeRN();
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor(false, condition);
		}
	}
	
	


	public static class DefaultNodeBuiderRi extends NodeBuilder.Single {

		
		public DefaultNodeBuiderRi(ICondition condition, boolean isFirst,
				boolean isLast) {
			super(condition, isFirst, isLast);
		}

		@Override
		public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
			DefaultSearchNodeRi nodeRi = (DefaultSearchNodeRi)node;
			nodeRi.addRule(ruleN);
			return false;
		}

		@Override
		public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node,
				int ruleN) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ISearchTreeNode createNode() {
			return new DefaultSearchNodeRi();
		}

		@Override
		public ConditionDescriptor makeDescriptor() {
			return new ConditionDescriptor(false, condition);		}
	}

	

}
