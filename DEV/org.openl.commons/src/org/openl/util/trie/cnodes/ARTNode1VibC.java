package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeVi;
import org.openl.util.trie.nodes.MappedArrayIterator;

public class ARTNode1VibC extends IARTNodeN.EmptyARTNodeN implements IARTNode, IARTNodeVi {

	int start;
	
	int[] values;
	byte[] mapper;

	
	public ARTNode1VibC(int start, byte[] mapper, int[] values) {
		this.start = start;
		this.mapper = mapper;
		this.values = values;
	}
	
	
	@Override
	public Object getValue(int index) {
		
		int idx = index - start;
		if (idx < 0 || idx >= mapper.length)
			return null;
		byte b = mapper[idx];
		if (b == 0)
			return null;
		return  values[(255 - b) & 0xff];  
	}


	@Override
	public void setValue(int index, Object value) {
		
		throw new UnsupportedOperationException();
		
	}


	@Override
	public int countV() {
		return values.length ;
	}

	@Override
	public int minIndexV() {
		return start;
	}

	@Override
	public int maxIndexV() {
		return start + values.length - 1;
	}
	@Override
	public IIntIterator indexIteratorV() {
		return MappedArrayIterator.iterator(start, mapper);
	}
	
	@Override
	public IARTNode compact() {
		return this;
	}
	

}
