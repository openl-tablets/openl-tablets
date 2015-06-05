package org.openl.util.trie.cnodes;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeV;

public class SingleNodeN  extends IARTNodeV.EmptyARTNodeV implements IARTNode {

	private int storedIndex;
	private IARTNode node;
	
	public SingleNodeN(int storedIndex, IARTNode node) {
		super();
		this.storedIndex = storedIndex;
		this.node = node;
	}


	@Override
	public IARTNode findNode(int index) {
		return index == storedIndex ? node : null;
	}

	@Override
	public void setNode(int index, IARTNode node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int countN() {
		return 1;
	}
	
	
	

	@Override
	public int minIndexN() {
		return storedIndex;
	}

	@Override
	public int maxIndexN() {
		return storedIndex;
	}




	@Override
	public IIntIterator indexIteratorN() {
		return AIntIterator.fromValue(storedIndex);
	}


	@Override
	public IARTNode compact() {
		node = node.compact();
		return this;
	}

}
