package org.openl.util.trie;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;

public interface IARTNodeN extends IARTNodeX {
	


	IARTNodeX findNode(int index);
	
	IARTNodeN setNode(int index, IARTNodeX node);
	

	int countN();
	int minIndexN();
	int maxIndexN();
	
	IIntIterator indexIteratorN();

	
	static public final IARTNodeN EMPTY = new EmptyARTNodeN();
	static class EmptyARTNodeN implements IARTNodeN {
		
		@Override
		public IARTNodeN setNode(int index, IARTNodeX node) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int minIndexN() {
			return 0;
		}
		
		@Override
		public int maxIndexN() {
			return 0;
		}
		
		@Override
		public IARTNodeX findNode(int index) {
			return null;
		}
		
		@Override
		public int countN() {
			return 0;
		}

		@Override
		public IIntIterator indexIteratorN() {
			return AIntIterator.fromValue();
		}

		@Override
		public IARTNodeX compact() {
			return this;
		}
	};
	
	
}
