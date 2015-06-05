package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.nodes.NodeArrayIterator;

public final class ARTNode1NVibC extends ARTNode1VibC {

	public ARTNode1NVibC(int startN, int countN, IARTNode[] nodes,
			int newStartV, byte[] newMapperV, int[] mappedArrayV) {
		super(newStartV, newMapperV, mappedArrayV);
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
