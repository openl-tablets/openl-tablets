package org.openl.util.trie;

public interface IARTNode{

	IARTNode findNode(int index);

	void setNode(int index, IARTNode node);

	Object getValue(int index);

	void setValue(int index, Object value);

	class EmptyARTNode implements IARTNode
	{

		@Override
		public void setValue(int index, Object value) {
			throw new UnsupportedOperationException();
		}
		

		@Override
		public Object getValue(int index) {
			return null;
		}

		@Override
		public void setNode(int index, IARTNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public IARTNode findNode(int index) {
			return null;
		}
	}

}
