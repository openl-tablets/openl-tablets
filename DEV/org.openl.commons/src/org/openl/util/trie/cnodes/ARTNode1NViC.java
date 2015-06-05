package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.nodes.NodeArrayIterator;

public final class ARTNode1NViC extends ARTNode1Vi {

	public ARTNode1NViC(int startN, int countN, IARTNode[] nodes,   int startV, int countV, int[] values) {
		super(startV, countV, values);
		this.nodes = nodes;
		this.startN = startN;
		this.countN = countN;
	}

	int startN;
	
	IARTNode[] nodes;

	private int countN;

	
	
	@Override
	public IARTNode findNode(int index) {
		int idx = index - startN;
		if (idx < 0 || idx >= nodes.length)
			return null;
		return nodes[idx];
	}


	@Override
	public void setNode(int index, IARTNode node) {
		nodes[index - startN] = node;
		
	}


	@Override
	public int countN() {
		return countN ;
	}

	@Override
	public int minIndexN() {
		return startN;
	}

	@Override
	public int maxIndexN() {
		return startN + nodes.length -1;
	}


	@Override
	public IIntIterator indexIteratorN() {
		return NodeArrayIterator.iterator(startN, nodes);
	}

}
