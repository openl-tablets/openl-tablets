package org.openl.util.trie.cnodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.IARTNodeVI;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.nodes.MappedArrayIterator;

public class ARTNode2VIbC implements IARTNodeVI {

	int start;
	
	int[] values;
	byte[] mapper;


	
	
	public ARTNode2VIbC(){}

	
	
	public ARTNode2VIbC(int start, byte[] mapper, int[] values) {
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
	public IARTNodeV setValue(int index, Object value) {
		
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
	public IARTNodeX compact() {
		return this;
	}
	

}
