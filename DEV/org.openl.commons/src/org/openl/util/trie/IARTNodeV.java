package org.openl.util.trie;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;

public interface IARTNodeV extends IARTNodeX {
	
	
	Object getValue(int index);
	
	IARTNodeV setValue(int index, Object value);
	
	int countV();
	int minIndexV();
	int maxIndexV();
	

	
	static public final  EmptyARTNodeV EMPTY = new EmptyARTNodeV();
	
	static class EmptyARTNodeV implements IARTNodeV {
		
		@Override
		public IARTNodeV setValue(int index, Object value) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int minIndexV() {
			return 0;
		}
		
		@Override
		public int maxIndexV() {
			return 0;
		}
		
		@Override
		public Object getValue(int index) {
			return null;
		}
		
		@Override
		public int countV() {
			return 0;
		}

		@Override
		public IIntIterator indexIteratorV() {
			return AIntIterator.fromValue();
		}

		@Override
		public IARTNodeX compact() {
			return this;
		}
	};
	
	
	IIntIterator indexIteratorV();
	
}
