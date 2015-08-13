package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.nodes.NodeArrayIterator;

public class ARTNode0N extends IARTNodeV.EmptyARTNodeV implements IARTNode {



	
	IARTNode[] nodes;

	private int countN;

	
	
	public ARTNode0N(int countN, IARTNode[] nodes)
	{
		this.nodes = nodes;
		this.countN = countN;
	}
	
	
	
	@Override
	public IARTNode findNode(int index) {
//		if (index < 0 || index >= nodes.length)
//			return null;
		return nodes[index];
	}


	@Override
	public void setNode(int index, IARTNode node) {
		++countN;
		nodes[index] = node;
	}

	@Override
	public int countN() {
		return countN ;
	}

	@Override
	public int minIndexN() {
		return 0;
	}

	@Override
	public int maxIndexN() {
		return nodes.length -1;
	}


	@Override
	public IIntIterator indexIteratorN() {
		return NodeArrayIterator.iterator(0, nodes);
	}


	@Override
	public IARTNode compact() { 
		return this;
	}

}
