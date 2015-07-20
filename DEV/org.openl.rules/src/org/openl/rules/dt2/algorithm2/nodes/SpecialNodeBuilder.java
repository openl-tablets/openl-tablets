package org.openl.rules.dt2.algorithm2.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dt2.algorithm2.ConditionDescriptor;
import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.NodeBuilder;
import org.openl.rules.dt2.element.ICondition;

public class SpecialNodeBuilder extends NodeBuilder {

	private NodeBuilder nodeBuilder;
	

	private NodeBuilder emptyBuilder;
	
	private NodeBuilder formulaBuilder;

	public SpecialNodeBuilder(ICondition condition, boolean isFirst,
			boolean isLast, NodeBuilder nodeBuilder, IndexInfo info) {
		super(condition, isFirst, isLast);
		this.nodeBuilder = nodeBuilder;
		this.emptyBuilder = EmptyNodeBuilder.makeBuilder(condition, isFirst, isLast);
		this.formulaBuilder = DefaultNodeBuilder.makeNodeBuilder(condition, isFirst, isLast, info);
	}

	@Override
	public boolean indexRuleN(ISearchTreeNode node, int ruleN) {
		SpecialSearchTreeNodeForBuild snode = (SpecialSearchTreeNodeForBuild) node;
		selectCurrentBuilder(snode  ,ruleN);
		
		return snode.currentBuilder.indexRuleN(snode.last(), ruleN);
	}

	private void selectCurrentBuilder(SpecialSearchTreeNodeForBuild snode, int ruleN) {
		
		NodeBuilder newBuilder = null;
		if (condition.isEmpty(ruleN))
			newBuilder = emptyBuilder;
		else if (condition.hasFormula(ruleN))
			newBuilder = formulaBuilder;
		else 
			newBuilder = nodeBuilder;
		
		if (newBuilder != snode.currentBuilder)
		{
			ISearchTreeNode newCurrentNode = newBuilder.createNode();
			snode.addNode(newCurrentNode);
			snode.currentBuilder = newBuilder;
		}	
		
	}

	@Override
	public boolean isSingleNode(int ruleN) {
		throw new UnsupportedOperationException("Should not call");
	}
	
	@Override
	public boolean isSingleNode(ISearchTreeNode node, int ruleN) {
		SpecialSearchTreeNodeForBuild snode = (SpecialSearchTreeNodeForBuild) node;

		selectCurrentBuilder(snode, ruleN);
		
		return snode.currentBuilder.isSingleNode(ruleN);
	}

	@Override
	public Iterator<ISearchTreeNode> findOrCreateNextNodes(
			ISearchTreeNode node, int ruleN) {
		SpecialSearchTreeNodeForBuild snode = (SpecialSearchTreeNodeForBuild) node;
		return snode.currentBuilder.findOrCreateNextNodes(snode.last(), ruleN);
	}

	@Override
	public ISearchTreeNode findOrCreateNextNode(ISearchTreeNode node, int ruleN) {
		SpecialSearchTreeNodeForBuild snode = (SpecialSearchTreeNodeForBuild) node;
		return snode.currentBuilder.findOrCreateNextNode(snode.last(), ruleN);
	}

	@Override
	public ISearchTreeNode createNode() {
		return new SpecialSearchTreeNodeForBuild();
	}

	@Override
	public ConditionDescriptor makeDescriptor() {
		return nodeBuilder.makeDescriptor();
	}

	
	
	
	
	@Override
	public void setNext(NodeBuilder next) {
		super.setNext(nodeBuilder);
		emptyBuilder.setNext(next);
		nodeBuilder.setNext(next);
		formulaBuilder.setNext(next);
	}





	static public class SpecialSearchTreeNodeForBuild extends BaseSearchNode {
		
		NodeBuilder currentBuilder;


		List<ISearchTreeNode> list = new ArrayList<ISearchTreeNode>();

		ISearchTreeNode last() {
			return list.size() == 0 ? null : list.get(list.size() - 1);
		}
		
		void addNode(ISearchTreeNode node)
		{
			list.add(node);
		}

		@Override
		public ISearchTreeNode compactSearchNode() {
			int n = list.size();
			ISearchTreeNode[] res = new ISearchTreeNode[n];
			for (int i = 0; i < n; i++) {
				res[i] = list.get(i).compactSearchNode();
			}
			list = Arrays.asList(res);
			this.currentBuilder = null;
			return this;
		}

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			return findFirstNodeOrValue(0, scxt);
		}

		public Object findFirstNodeOrValue(int start, SearchContext scxt) {
			for (int i = start; i < list.size(); i++) {
				Object res = list.get(i).findFirstNodeOrValue(scxt);
				if (res != null)
				{
					scxt.store(new SaveIndex(i, scxt.retrieve()));
					return res;
				}	
				
			}
			
			return null;
		}
		
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			SaveIndex si = (SaveIndex) scxt.retrieve();
			int index = si.index;
			scxt.store(si.save);
			Object res = list.get(index).findNextNodeOrValue(scxt);
			if (res != null)
			{
				scxt.store(new SaveIndex(index, scxt.retrieve()));
				return res;
			}
			
			return findFirstNodeOrValue(index + 1, scxt);
			
		}

	}

	
	
	

	
	static class SaveIndex
	{
		int index;
		public SaveIndex(int index, Object save) {
			super();
			this.index = index;
			this.save = save;
		}
		Object save;
	}
	
}
