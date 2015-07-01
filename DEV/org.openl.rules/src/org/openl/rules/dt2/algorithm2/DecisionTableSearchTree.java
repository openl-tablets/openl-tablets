package org.openl.rules.dt2.algorithm2;

import org.openl.domain.IIntIterator;
import org.openl.rules.dt2.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dtx.trace.IDecisionTableTraceObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.TraceStack;

public class DecisionTableSearchTree implements IDecisionTableAlgorithm {

	ConditionDescriptor[] descriptors;

	ISearchTreeNode root;

	private IndexInfo info;

	public DecisionTableSearchTree(ISearchTreeNode root, ConditionDescriptor[] descriptors, IndexInfo info) {
		this.root = root;
		this.descriptors = descriptors;
		this.info = info;
	}

	@Override
	public void removeParamValuesForIndexedConditions() {
		// TODO Auto-generated method stub

	}

	@Override
	public IIntIterator checkedRules(Object target, Object[] params,
			IRuntimeEnv env) {

        if (root == null)
            return info.makeRuleIterator();

		
		SearchContext scxt = new SearchContext(target, params, env);

		return searchFirst(scxt);
	}

	public SearchResult searchNext(SearchContext scxt) {

		Object next = backtrack(scxt);
		if (next == null)
			return SearchResult.notFound(scxt);

		while (true) {

			if (scxt.currentConditionIdx + 1 == descriptors.length) // last node
			{
				scxt.setValue(next);
				return SearchResult.found(scxt);
			} else
				scxt.setNextNode(next);
			
			next = scxt.currentNode().findNextNodeOrValue(scxt);
			if (next == null) {
				--scxt.currentConditionIdx;
				next = backtrack(scxt);
				if (next == null)
					return SearchResult.notFound(scxt);

			}
		}	
	}

	public SearchResult searchFirst(SearchContext scxt) {

		while (true) {
			Object next = scxt.currentNode().findFirstNodeOrValue(scxt);
			if (next == null) {
				--scxt.currentConditionIdx;
				next = backtrack(scxt);
				if (next == null)
					return SearchResult.notFound(scxt);
			} else {

				if (scxt.currentConditionIdx + 1 == descriptors.length) // last
																		// node
				{
					scxt.setValue(next);
					return SearchResult.found(scxt);
				} else
					scxt.setNextNode(next);

			}

		}

	}



	private Object backtrack(SearchContext scxt) {

		while (scxt.currentConditionIdx >= 0) {
			ISearchTreeNode currentNode = scxt.currentNode();
			Object next = currentNode.findNextNodeOrValue(scxt);
			if (next != null)
				return next;
			--scxt.currentConditionIdx;
		}

		return null;
	}

	@Override
	public IDecisionTableAlgorithm asTraceDecorator(TraceStack conditionsStack,
			IDecisionTableTraceObject traceObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public final class SearchContext extends RuntimeContext {

		public int currentConditionIdx;
		private ISearchTreeNode[] savedNodes;
		Object[] indexedValues;
		
		public int savedRuleN;

		public SearchContext(Object target, Object[] params, IRuntimeEnv env) {
			super(target, params, env);
			int len = descriptors.length;
			indexedValues = new Object[len];
			savedNodes = new ISearchTreeNode[len];
			savedNodes[0] = root;
		}

		public void setNextNode(Object next) {
			savedNodes[++currentConditionIdx] = (ISearchTreeNode) next;
		}

		public void setValue(Object next) {
			savedRuleN = (Integer) next;
		}

		public final ISearchTreeNode currentNode() {
			return savedNodes[currentConditionIdx];
		}

		public SearchResult findNext() {
			return searchNext(this);
		}
		
		
		public Object getIndexedValue()
		{
			if (indexedValues[currentConditionIdx] == null)
			{
				ConditionDescriptor cd = descriptors[currentConditionIdx]; 
				if (!cd.useIndexedValue)
					return cd.evaluate(this);
				indexedValues[currentConditionIdx] = cd.evaluate(this);
				
			}	
			return indexedValues[currentConditionIdx];
			
			
			
		}

	}

}
