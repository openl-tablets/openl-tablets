package org.openl.util.trie.cnodes;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeVi;

public class SingleNodeVi extends IARTNodeN.EmptyARTNodeN implements IARTNode, IARTNodeVi {

	private int storedIndex;
	private int value;
	
	public SingleNodeVi(int storedIndex, int value) {
		super();
		this.storedIndex = storedIndex;
		this.value = value;
	}


	@Override
	public Integer getValue(int index) {
		return index == storedIndex ? value : null;
	}

	@Override
	public void setValue(int index, Object value) {
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
	public IARTNode compact() {
		return this;
	}

}
