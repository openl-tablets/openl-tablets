package org.openl.util.trie;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;

public interface IARTNodeN extends IARTNodeX {
	


	IARTNode findNode(int index);
	
	void setNode(int index, IARTNode node);
	

	int countN();
	int minIndexN();
	int maxIndexN();
	
	IIntIterator indexIteratorN();

	
	static public final IARTNodeN EMPTY = new EmptyARTNodeN();
	static public class EmptyARTNodeN implements IARTNodeN {
		
		@Override
		public void setNode(int index, IARTNode node) {
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
		public IARTNode findNode(int index) {
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

	};
	
	
}
