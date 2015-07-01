package org.openl.rules.dt2.algorithm2.nodes;

import org.openl.rules.dt2.algorithm2.ISearchTreeNode;
import org.openl.util.trie.IARTNode;

public abstract class BaseSearchNode extends IARTNode.EmptyARTNode implements ISearchTreeNode {

	@Override
	public IARTNode compact() {
		return compactSearchNode();
	}

}
