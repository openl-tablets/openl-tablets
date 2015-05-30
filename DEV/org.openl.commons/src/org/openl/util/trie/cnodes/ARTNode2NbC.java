package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.nodes.MappedArrayIterator;

public class ARTNode2NbC implements IARTNodeN {



	int start;
	byte[] mapper;
	
	IARTNodeX[] nodes;

	public ARTNode2NbC(int start, byte[] mapper, IARTNodeX[] nodes) {
		super();
		this.start = start;
		this.nodes = nodes;
		this.mapper = mapper;
	}


	
	
	
	
	
	@Override
	public IARTNodeX findNode(int index) {
		
		int idx = index - start;
		if (idx < 0 || idx >= mapper.length)
			return null;
		byte b = mapper[idx];
		if (b == 0)
			return null;
		return  nodes[(255 - b) & 0xff];  
	}


	@Override
	public IARTNodeN setNode(int index, IARTNodeX node) {
		
		throw new UnsupportedOperationException();
	}



	@Override
	public int countN() {
		return nodes.length ;
	}

	@Override
	public int minIndexN() {
		return start;
	}

	@Override
	public int maxIndexN() {
		return start + nodes.length - 1;
	}
	
	@Override
	public IIntIterator indexIteratorN() {
		return MappedArrayIterator.iterator(start, mapper);
	}
	
	public IARTNodeX compact() {
		return this;
	}
	
}
