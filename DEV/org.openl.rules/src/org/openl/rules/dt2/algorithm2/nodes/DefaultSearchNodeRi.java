package org.openl.rules.dt2.algorithm2.nodes;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.nodes.NodesUtil.RuleRange;

public class DefaultSearchNodeRi extends BaseSearchNode {

	List<Integer> rules = new ArrayList<Integer>();

	public DefaultSearchNodeRi() {
		super();

	}

	@Override
	public ISearchTreeNode compactSearchNode() {
		Object intseq = NodesUtil.compactSequence(rules);

		if (intseq instanceof Integer) {
			return new SingleRi((Integer) intseq);
		}

		if (intseq instanceof RuleRange) {
			return new RangeRi((RuleRange) intseq);
		}

		return new ArrayRi((int[]) intseq);
	}

	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {

		int i = 0;
		while (i <= rules.size()) {
			int ruleN = rules.get(i);
			if (scxt.calculateCondition(ruleN)) {
				scxt.store(i);
				return ruleN;
			}
			++i;
		}

		return null;
	}

	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		int i = (Integer) scxt.retrieve();
		while (i <= rules.size()) {
			int ruleN = rules.get(i);
			if (scxt.calculateCondition(ruleN)) {
				scxt.store(i);
				return ruleN;
			}
			++i;
		}

		return null;
	}

	public void addRule(int ruleN) {
		rules.add(ruleN);
	}

	/********************** Compact Classes ****************************/

	static class SingleRi extends CompactUnique {
		public SingleRi(int ruleN) {
			super();
			this.ruleN = ruleN;
		}

		int ruleN;

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			return scxt.calculateCondition(ruleN) ? ruleN : null;
		}

	}

	static class RangeRi extends Compact {
		public RangeRi(RuleRange ruleRange) {
			super();
			this.ruleRange = ruleRange;
		}

		RuleRange ruleRange;

		@Override
		public Object findFirstNodeOrValue(SearchContext scxt) {
			return findNodeOrValue(scxt, ruleRange.from);
		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			return findNodeOrValue(scxt, (Integer)scxt.retrieve());
		}

		private Object findNodeOrValue(SearchContext scxt, int start) {
			for (int ruleN = start; ruleN <= ruleRange.to; ruleN += ruleRange.step) {
				if (scxt.calculateCondition(ruleN))
				{	
					scxt.store(ruleN);
					return ruleN;
				}	
			}

			return null;
		}
	}

	static class ArrayRi extends Compact {
		public ArrayRi(int[] ruleAry) {
			super();
			this.ruleAry = ruleAry;
		}

		int[] ruleAry;

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
					return idx;
				}	
			}

			return null;
		}
	}
}
