package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.nodes.NodeArrayIterator;

public class ARTNode1N implements IARTNodeN {



	int start;
	
	IARTNodeX[] nodes;

	private int count;

	
	
	public ARTNode1N(int start, int count, IARTNodeX[] nodes)
	{
		this.nodes = nodes;
		this.start = start;
		this.count = count;
	}
	
	public ARTNode1N(int start, int count, int capacity)
	{
		this.start = start;
		this.count = count;
		this.nodes = new IARTNodeX[capacity];
	}
	
	
	
	@Override
	public IARTNodeX findNode(int index) {
		int idx = index - start;
		if (idx < 0 || idx >= nodes.length)
			return null;
		return nodes[idx];
	}


	@Override
	public IARTNodeN setNode(int index, IARTNodeX node) {
		nodes[index - start] = node;
		
		return this;
		
	}


	



	@Override
	public int countN() {
		return count ;
	}

	@Override
	public int minIndexN() {
		return start;
	}

	@Override
	public int maxIndexN() {
		return start + nodes.length -1;
	}


	@Override
	public IIntIterator indexIteratorN() {
		return NodeArrayIterator.iterator(start, nodes);
	}


	@Override
	public IARTNodeX compact() { 
		return this;
	}

}
