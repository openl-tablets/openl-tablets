package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.nodes.MappedArrayIterator;

public final class ARTNode1NbViC extends ARTNode1Vi {

	
	int startN;
	byte[] mapperN;
	
	IARTNode[] nodes;

	public ARTNode1NbViC(int startN, byte[] mapperN, IARTNode[] nodes, int startV, int countV, int[] valuesV) {
		super(startV, countV, valuesV);
		this.startN = startN;
		this.nodes = nodes;
		this.mapperN = mapperN;
	}


	
	
	
	
	
	@Override
	public IARTNode findNode(int index) {
		
		int idx = index - startN;
		if (idx < 0 || idx >= mapperN.length)
			return null;
		byte b = mapperN[idx];
		if (b == 0)
			return null;
		return  nodes[(255 - b) & 0xff];  
	}


	@Override
	public void setNode(int index, IARTNode node) {
		
		throw new UnsupportedOperationException();
	}



	@Override
	public int countN() {
		return nodes.length ;
	}

	@Override
	public int minIndexN() {
		return startN;
	}

	@Override
	public int maxIndexN() {
		return startN + nodes.length - 1;
	}
	
	@Override
	public IIntIterator indexIteratorN() {
		return MappedArrayIterator.iterator(startN, mapperN);
	}
	
	
	
}
