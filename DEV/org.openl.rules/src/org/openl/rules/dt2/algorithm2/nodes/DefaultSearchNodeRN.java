package org.openl.rules.dt2.algorithm2.nodes;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.nodes.NodesUtil.RuleRange;
import org.openl.rules.dt2.element.ICondition;

public class DefaultSearchNodeRN extends BaseSearchNode {

	
	
	
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
				return new SingleRN((Integer) intseq, nodes.get(0));
			}

			if (intseq instanceof RuleRange) {
				return new RangeRN((RuleRange) intseq, nodes.toArray(new ISearchTreeNode[0]));
			}

			return new ArrayRN((int[]) intseq, nodes.toArray(new ISearchTreeNode[0]));
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
		if (ranges.size() == 0)
			return null;
		
		RuleRange rlast = ranges.get(ranges.size() - 1);
		
		if (ruleN != rlast.to + info.getStep())
			return null;
		
		if (!condition.isEqual(rlast.to, ruleN))
			return null;
		
		rlast.to = ruleN;
		
		return nodes.get(nodes.size() - 1);
		
		
	}

	public void addNewNode(int ruleN, ISearchTreeNode nextNode) {
		ranges.add(new RuleRange(ruleN));
		nodes.add(nextNode);
	}
	
	
	/********************** Compact Classes ****************************/

	static class SingleRN extends CompactUnique {

		int ruleN;
		ISearchTreeNode node;

		public SingleRN(int ruleN, ISearchTreeNode node) {
			super();
			this.ruleN = ruleN;
			this.node = node;
		}

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			return scxt.calculateCondition(ruleN) ? node : null;
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
			return findNodeOrValue(scxt, 0);
		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			return findNodeOrValue(scxt, (Integer)scxt.retrieve());
		}

		private Object findNodeOrValue(SearchContext scxt, int step) {
			for (; step < nodes.length; step++) {
				int ruleN = ruleRange.from + step * ruleRange.step;
				if (scxt.calculateCondition(ruleN))
				{	
					scxt.store(step);
					return nodes[step];
				}	
			}

			return null;
		}
	}

	static class ArrayRN extends Compact {

		int[] ruleAry;
		ISearchTreeNode[] nodes;

		public ArrayRN(int[] ruleAry, ISearchTreeNode[] nodes) {
			super();
			this.ruleAry = ruleAry;
			this.nodes = nodes;
		}

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			return findNodeOrValue(scxt, 0);
		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			return findNodeOrValue(scxt, (Integer)scxt.retrieve());
		}

		private Object findNodeOrValue(SearchContext scxt, int start) {
			for (int idx = start; idx <= ruleAry.length; ++idx) {
				
				if (scxt.calculateCondition(ruleAry[idx]))
				{	
					scxt.store(idx);
					return nodes[idx];
				}	
			}

			return null;
		}
	}
}
	
	
