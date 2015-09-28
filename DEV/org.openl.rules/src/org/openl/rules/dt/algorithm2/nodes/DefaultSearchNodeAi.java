package org.openl.rules.dt.algorithm2.nodes;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;
import org.openl.rules.dt.algorithm2.ISearchTreeNode;


public class DefaultSearchNodeAi extends BaseSearchNode {

	private int from;
	private int to;
	private int step;

	public DefaultSearchNodeAi(int from, int to, int step) {
		super();
		this.from = from;
		this.to = to;
		this.step = step;
	}


	@Override
	public ISearchTreeNode compactSearchNode() {
		return this;
	}

	@Override
	public Object findFirstNodeOrValue(SearchContext scxt) {
		int cnt = from;
		while(cnt <= to)
		{
			if (scxt.calculateCondition(cnt))
				return cnt;
			cnt += step;
		}	
		
		return null;
	}

	@Override
	public Object findNextNodeOrValue(SearchContext scxt) {
		int cnt = scxt.savedRuleN + 1;
		while(cnt < to)
		{
			if (scxt.calculateCondition(cnt))
				return cnt;
			++cnt;
		}	
		
		return null;
	}


}
