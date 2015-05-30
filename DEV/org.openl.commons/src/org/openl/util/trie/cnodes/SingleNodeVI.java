package org.openl.util.trie.cnodes;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.IARTNodeVI;
import org.openl.util.trie.IARTNodeX;

public class SingleNodeVI implements IARTNodeVI {

	private int storedIndex;
	private int value;
	
	public SingleNodeVI(int storedIndex, int value) {
		super();
		this.storedIndex = storedIndex;
		this.value = value;
	}


	@Override
	public Integer getValue(int index) {
		return index == storedIndex ? value : null;
	}

	@Override
	public IARTNodeV setValue(int index, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int countV() {
		return 1;
	}
	
	
	

	@Override
	public int minIndexV() {
		return storedIndex;
	}

	@Override
	public int maxIndexV() {
		return storedIndex;
	}




	@Override
	public IIntIterator indexIteratorV() {
		return AIntIterator.fromValue(storedIndex);
	}


	@Override
	public IARTNodeX compact() {
		return this;
	}

}
