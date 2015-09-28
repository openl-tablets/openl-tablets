package org.openl.rules.dt.algorithm2;

import org.openl.rules.dt.algorithm2.DecisionTableSearchTree.SearchContext;

public interface ISearchNode {

	Object findFirstNodeOrValue(SearchContext scxt);

	Object findNextNodeOrValue(SearchContext scxt);


}
