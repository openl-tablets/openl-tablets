package org.openl.util.trie.cnodes;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeX;

public class SingleNodeN implements IARTNodeN {

	private int storedIndex;
	private IARTNodeX node;
	
	public SingleNodeN(int storedIndex, IARTNodeX node) {
		super();
		this.storedIndex = storedIndex;
		this.node = node;
	}


	@Override
	public IARTNodeX findNode(int index) {
		return index == storedIndex ? node : null;
	}

	@Override
	public IARTNodeN setNode(int index, IARTNodeX node) {
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
	public IARTNodeX compact() {
		node = node.compact();
		return this;
	}

}
