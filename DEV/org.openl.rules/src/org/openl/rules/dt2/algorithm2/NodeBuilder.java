package org.openl.rules.dt2.algorithm2;

import java.util.Iterator;

import org.openl.rules.dt2.element.ICondition;

public abstract class NodeBuilder   {
	
	public ICondition condition;
	public NodeBuilder(ICondition condition, boolean isFirst, boolean isLast) {
		super();
		this.condition = condition;
		this.isFirst = isFirst;
		this.isLast = isLast;
	}

	boolean isFirst;
	boolean isLast;
	public NodeBuilder next;
	

	public abstract boolean indexRuleN(ISearchTreeNode node, int ruleN);

	public abstract boolean isSingleNode(int ruleN);

	public abstract Iterator<ISearchTreeNode> findOrCreateNextNodes(ISearchTreeNode node, int ruleN);

	public abstract ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node, int ruleN);

	public abstract ISearchTreeNode createNode();

	public abstract  ConditionDescriptor makeDescriptor();
	
	
	
	public abstract static class Single extends NodeBuilder
	{

		public Single(ICondition condition, boolean isFirst, boolean isLast) {
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
