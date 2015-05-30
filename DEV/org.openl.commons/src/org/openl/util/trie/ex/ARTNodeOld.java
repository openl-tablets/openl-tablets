package org.openl.util.trie.ex;

public abstract class ARTNodeOld {

	public int prefixLen(){return 0;}
	protected ARTNodeOld[] child;
	
	public int count = 0;
	
	ARTNodeOld(int capacity)
	{
		this.child = new ARTNodeOld[capacity];
	}
	


	static class SmallNode extends ARTNodeOld {
		
		private byte[] key;
		
		public SmallNode(int capacity)
		{
			super(capacity);
			this.key = new byte[capacity];
		}

		
		

		@Override
		public int capacity() {
			return child.length;
		}



		@Override
		protected int childIndex(byte byteKey) {
			for (int i = 0; i < count; ++i)
			{	
				if (this.key[i] == byteKey)
					return i;
			}	
			return -1;
		}




		@Override
		protected int getNewChildIndex(byte byteKey) {
			return count;
		}
	}	
	
	


	
	

	static class MappingNode extends ARTNodeOld {

		private byte[] indexMap;

		
		public MappingNode(int maxKeySize, int maxChildSize)
		{
			
			super(maxChildSize);
			assert(maxKeySize <= 256);
			assert(maxChildSize < Byte.MAX_VALUE);
			this.indexMap = new byte[maxKeySize];
			
		}
		

		@Override
		public int capacity() {
			return child.length;
		}

		@Override
		protected int childIndex(byte byteKey) {
			return indexMap[byteKey < 0 ? 127 - byteKey : byteKey] - 1;
		}


		@Override
		protected int getNewChildIndex(byte byteKey) {
			int index = count;
			indexMap[byteKey < 0 ? 127 - byteKey : byteKey] = (byte)(index + 1);
			return index;
		}

	}

	static class DirectNode extends ARTNodeOld {


		public DirectNode(int maxKeySize)
		{
			super(maxKeySize);
		}
		
		@Override
		protected int childIndex(byte byteKey) {
			return byteKey < 0 ? 127 - byteKey : byteKey;
		}
		

		@Override
		public int capacity() {
			return child.length;
		}

		@Override
		protected int getNewChildIndex(byte byteKey) {
			return childIndex(byteKey);
		}


	}

	public boolean isFull() {
		return count >= capacity();
	}

	public abstract int capacity();

	public void replace(ARTNodeOld newNode, byte byteKey) {
		child[childIndex(byteKey)] = newNode;
	}

	public ARTNodeOld findChild(byte byteKey) {
		int index = childIndex(byteKey);
		if (index < 0)
			return null;
		return child[index];
	}

	protected abstract int childIndex(byte byteKey);

	public void addChild(ARTNodeOld next, byte byteKey, int value)
	{
		int index = getNewChildIndex(byteKey);
		child[index] = next;
		++count;
	}

	protected abstract  int getNewChildIndex(byte byteKey);

}
