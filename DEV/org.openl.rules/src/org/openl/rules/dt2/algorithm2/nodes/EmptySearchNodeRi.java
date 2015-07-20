package org.openl.rules.dt2.algorithm2.nodes;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.rules.dt2.algorithm2.nodes.NodesUtil.RuleRange;

public class EmptySearchNodeRi extends BaseSearchNode {

	List<Integer> rules = new ArrayList<Integer>();

	public EmptySearchNodeRi() {
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
		throw new UnsupportedOperationException();
	}

	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		throw new UnsupportedOperationException();
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
			return ruleN;
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
			scxt.store(ruleRange.from);
			return ruleRange.from;					

		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			int ruleN = (Integer)scxt.retrieve() + ruleRange.step;
			if (ruleN >= ruleRange.to)
				return null;
			scxt.store(ruleN);
			return ruleN;
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
			scxt.store(0);
			return ruleAry[0];					
		}
		
		@Override
		public Object findNextNodeOrValue(SearchContext scxt) {
			int idx = (Integer)scxt.retrieve() + 1;
			if (idx >= ruleAry.length)
				return null;
			scxt.store(idx);
			return ruleAry[idx];
		}
	}
}
