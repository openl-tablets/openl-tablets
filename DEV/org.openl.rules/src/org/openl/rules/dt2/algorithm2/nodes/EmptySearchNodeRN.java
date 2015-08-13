package org.openl.rules.dt2.algorithm2.nodes;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.nodes.NodesUtil.RuleRange;
import org.openl.rules.dt2.element.ICondition;

public class EmptySearchNodeRN extends BaseSearchNode {

	
	
	
	List<ISearchTreeNode> nodes = new ArrayList<ISearchTreeNode>();
	List<RuleRange> ranges = new ArrayList<RuleRange>();
	
	
	@Override
	public ISearchTreeNode compactSearchNode() {
		
			//prepare data
			int commonSize = ranges.size();
			List<Integer> rules = new ArrayList<Integer>(commonSize);
			for (int i = 0 ; i < ranges.size(); ++i) {
				nodes.set(i, nodes.get(i).compactSearchNode());
				rules.add(ranges.get(i).from);
			}
		
			Object intseq = NodesUtil.compactSequence(rules);

			if (intseq instanceof Integer) {
				return new SingleRN(nodes.get(0));
			}


			return new ArrayRN(nodes.toArray(new ISearchTreeNode[0]));
	}

	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {
		

		int n = ranges.size();
		for (int i = 0; i < n ; i++) {
			RuleRange rr = ranges.get(i);
			if (scxt.calculateCondition(rr.from))
			{	
				scxt.store(i);
				return nodes.get(i);
			}	
		}
		
		return null;
	}

	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		int n = ranges.size();
		for (int i = (Integer)scxt.retrieve(); i < n ; i++) {
			RuleRange rr = ranges.get(i);
			if (scxt.calculateCondition(rr.from))
			{	
				scxt.store(i);
				return nodes.get(i);
			}	
		}
		
		return null;
	}

	public ISearchTreeNode useLastNode(int ruleN, IndexInfo info, ICondition condition) {
		
		RuleRange rlast = ranges.get(ranges.size() - 1);
		
		if (ruleN != rlast.to + info.getStep())
			return null;
		
		
		rlast.to = ruleN;
		
		return nodes.get(nodes.size() - 1);
		
		
	}

	public void addNewNode(int ruleN, ISearchTreeNode nextNode) {
		ranges.add(new RuleRange(ruleN));
		nodes.add(nextNode);
	}
	
	
	/********************** Compact Classes ****************************/

	static class SingleRN extends Unique {

		ISearchTreeNode node;

		public SingleRN(ISearchTreeNode node) {
			super();
			this.node = node;
		}

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			return node;
		}

		@Override
		public ISearchTreeNode compactSearchNode() {
			node = node.compactSearchNode();
			return this;
		}

	}

	static class RangeRN extends Compact {
		RuleRange ruleRange;
		ISearchTreeNode[] nodes;

		public RangeRN(RuleRange ruleRange, ISearchTreeNode[] nodes) {
			super();
			this.ruleRange = ruleRange;
			this.nodes = nodes;
		}

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			scxt.store(0);
			return nodes[0];
		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			int idx = (Integer)scxt.retrieve() + ruleRange.step;
			if (idx < ruleRange.to)
			{
				scxt.store(idx);
				return nodes[idx];
				
			}	
			return null;
		}

	}

	static class ArrayRN extends Compact {

		ISearchTreeNode[] nodes;

		public ArrayRN(ISearchTreeNode[] nodes) {
			super();
			this.nodes = nodes;
		}

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			scxt.store(0);
			return nodes[0];
		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			int idx = (Integer)scxt.retrieve() + 1;
			if (idx < nodes.length)
			{
				scxt.store(idx);
				return nodes[idx];
			}	
			return null;
		}

	}
}
	
	
