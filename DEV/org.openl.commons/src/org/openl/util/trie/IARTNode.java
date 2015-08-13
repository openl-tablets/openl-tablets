package org.openl.util.trie;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;

public interface IARTNode extends IARTNodeV, IARTNodeN {
	
	IARTNode compact();
	
	static public class EmptyARTNode extends EmptyARTNodeN implements IARTNode
	{

		@Override
		public void setValue(int index, Object value) {
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
		public IARTNode compact() {
			return this;
		}
	}

}
