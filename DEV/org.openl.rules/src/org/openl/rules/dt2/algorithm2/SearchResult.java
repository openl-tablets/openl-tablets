package org.openl.rules.dt2.algorithm2;

import org.openl.domain.AIntIterator;
import org.openl.rules.dt2.algorithm2.DecisionTableSearchTree.SearchContext;

public class SearchResult extends AIntIterator  {
	
	boolean notFound;
	
	public SearchResult(boolean notFound, SearchContext scxt) {
		super();
		this.notFound = notFound;
		this.scxt = scxt;
	}


	boolean needsCheck;
	SearchContext scxt;

	@Override
	public int nextInt() {
		needsCheck = true;
		return scxt.savedRuleN;
	}

	@Override
	public boolean isResetable() {
		return false;
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException("reset");
	}

	@Override
	public boolean hasNext() {
		if (notFound)
			return false;
		if (needsCheck)
		{
			SearchResult s = scxt.findNext();
			return !s.notFound;
		}
		
		return true;
	}
	
	
	static public SearchResult notFound(SearchContext scxt)
	{
		return new SearchResult(true, scxt);
	}
	
	static public SearchResult found(SearchContext scxt)
	{
		return new SearchResult(false, scxt);
	}

}
