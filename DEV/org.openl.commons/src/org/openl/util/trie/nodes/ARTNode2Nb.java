package org.openl.util.trie.nodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.cnodes.ARTNode1N;
import org.openl.util.trie.cnodes.ARTNode2NbC;
import org.openl.util.trie.cnodes.NodeFactory;
import org.openl.util.trie.cnodes.SingleNodeN;

public class ARTNode2Nb implements IARTNodeN {


	private static final int MAXIMUM_CAPACITY = 255;

	int start;
	
	IARTNodeX[] nodes;
	byte[] mapper;

	private int count = 0;

	private int minIndex;

	private int maxIndex;
	
	
	public ARTNode2Nb(){}
	public ARTNode2Nb(int start, int capacity, int mapCapacity)
	{
		init(start, capacity, mapCapacity);
	}
	
	
	public void init(int start, int capacity, int mapCapacity)
	{
		nodes = new IARTNodeX[capacity];
		mapper = new byte[mapCapacity];
		this.start = start;
		minIndex = start + mapCapacity;
		maxIndex = start;
	}
	
	
	@Override
	public IARTNodeX findNode(int index) {
		if (mapper == null)
			return null;
		
		int idx = index - start;
		if (idx < 0 || idx >= mapper.length)
			return null;
		byte b = mapper[idx];
		if (b == 0)
			return null;
		return  nodes[(255 - b) & 0xff];  
	}


	@Override
	public IARTNodeN setNode(int index, IARTNodeX node) {
		
		if (mapper == null)
			init(index);
		
		int idx = index - start;
		
		if (idx < 0) 
			return growNodeDown(index).setNode(index, node);
		
		if (idx >= mapper.length)
			return growMapperUp(index).setNode(index, node);
		
		
		
		if (mapper[idx] == 0)
		{
			if (count  >= nodes.length){
				return growCapacity().setNode(index, node);
			}
			
			minIndex = Math.min(index, minIndex);
			maxIndex = Math.max(index, maxIndex);
			nodes[count++] = node;
			mapper[idx] = (byte)(256 - count);
		}	
		
		else {
			byte b = mapper[idx];
			nodes[(255 - b) & 0xff] = node;
			}
		
		
		return this;
		
	}

	private void init(int index) {
		
		if ('a' <= index && index <= 'z')
		{
			init('a', 10, 'z' - 'a' + 1);
		}	
		else if ('A' <= index && index <= 'Z')
		{
			init('A', 10, 'Z' - 'A' + 1);
		}	
		else if ('0' <= index && index <= '9')
		{
			init('0', 5, '9' - '0' + 1);
		}
		else
		{
			int startX = Math.max(index - 10, 0);
			init(startX, 10, 40);
		}	
	}


	private IARTNodeN growCapacity() {
		int oldCapacity = nodes.length;
		int newCapacity = newCapacity(oldCapacity * 2);
		if (newCapacity <= oldCapacity)
			throw new RuntimeException("Node capacity overflow");
		
		IARTNodeX[] newNodes = new IARTNodeX[newCapacity];
		System.arraycopy(nodes, 0, newNodes, 0, oldCapacity);
		nodes = newNodes;
		
		return this;
	}


	private IARTNodeN growMapperUp(int index) {
		
		int oldCapacity = mapper.length;
		
		int newRequiredCapacity = index - minIndex + 1;
		
		if (newRequiredCapacity <= oldCapacity)
		{
			int margin = (oldCapacity - newRequiredCapacity) / 2;
			int newStart = minIndex - margin;
			int shift = newStart - start; 
			int oldOrigin = minIndex - start;
			int oldSize = maxIndex - minIndex + 1;
			System.arraycopy(mapper, oldOrigin , mapper, margin, oldSize);
			int cleanSize = Math.min(shift, oldSize);
			for (int i = 0; i < cleanSize; i++) {
				mapper[maxIndex - start - i] = 0;
			}
			start = newStart;
			return this;
		}	
	
		
		
		
		
		int newCapacity = newCapacity( Math.max(oldCapacity  * 2, newRequiredCapacity));
		
		if (newCapacity < newRequiredCapacity)
			throw new RuntimeException("Node capacity Overflow");
		
		
		byte[] newMapper = new byte[newCapacity];
		int margin = (newCapacity - newRequiredCapacity) / 2;
		int newStart = minIndex - margin;
		int oldMargin = minIndex - start;
		int oldSize = maxIndex - minIndex + 1;
		System.arraycopy(mapper, oldMargin , newMapper, margin, oldSize);
		start = newStart;
		mapper = newMapper;
		
		return this;
	}

	private IARTNodeN growNodeDown(int index) {
		int oldCapacity = mapper.length;
		
		int newRequiredCapacity = maxIndex - index + 1;

		int margin = (oldCapacity - newRequiredCapacity) / 2;
		int newStart = index - margin;
		int shift = start - newStart;
		int oldOrigin = minIndex - start;
		int newOrigin = oldOrigin + shift;
		int oldSize = maxIndex - minIndex + 1;
		
		
		if (newRequiredCapacity <= oldCapacity)
		{
			System.arraycopy(mapper, oldOrigin , mapper, newOrigin, oldSize);
			int cleanSize =  Math.min(shift, oldSize);
			for (int i = 0; i < cleanSize; i++) {
				mapper[oldOrigin + i] = 0;
			}
			start = newStart;
			return this;
		}	
	
		
		
		
		
		int newCapacity = newCapacity( Math.max(oldCapacity  * 2, newRequiredCapacity));
		
		if (newCapacity < newRequiredCapacity)
			throw new RuntimeException("Node capacity Overflow");
		
		
		byte[] newMapper = new byte[newCapacity];
		margin = (newCapacity - newRequiredCapacity) / 2;
		newStart = minIndex - margin;
		shift = start - newStart;
		newOrigin = oldOrigin + shift;
		
		System.arraycopy(mapper, oldOrigin , newMapper, newOrigin, oldSize);
		start = newStart;
		mapper = newMapper;
		
		return this;
	
	}
	

	public int newCapacity(int required)
	{
	
	  if (required < 0)
          throw new IllegalArgumentException("Illegal initial capacity: " +
                                             required);
      if (required > MAXIMUM_CAPACITY)
          required = MAXIMUM_CAPACITY;


      return required;
      
	}


	@Override
	public int countN() {
		return count ;
	}

	@Override
	public int minIndexN() {
		return minIndex;
	}

	@Override
	public int maxIndexN() {
		return maxIndex;
	}
	
	@Override
	public IIntIterator indexIteratorN() {
		return MappedArrayIterator.iterator(start, mapper);
	}
	
	public IARTNodeX compact() {
		switch (count) {
		case 0:
			return IARTNodeN.EMPTY;
			
		case 1:
			return new SingleNodeN(minIndex, nodes[0].compact());
		}
		
		int range = maxIndex - minIndex + 1;
		boolean useMapper = NodeFactory.useMapper(range,  byte[].class, count, Object[].class); 
		
		if (useMapper)
			return makeMappedCompact(range);
		
		
		return makeArrayCompact(range);
	}
	
	
	private IARTNodeN makeArrayCompact(int range) {
		
		IARTNodeX[] res = new IARTNodeX[range];
		
		for (int i = minIndex; i <= maxIndex; i++) {
			byte b = mapper[i - start];
			if (b != 0)
			{
				res[i - minIndex] = nodes[(255 - b) & 0xff].compact();
			}	
			
		}
		return new ARTNode1N(minIndex, count, res);
	}
	
	public IARTNodeN makeMappedCompact(int range) {
		
		
		int newStart = start;
		byte[] newMapper = mapper;	
		if (((range + 7) & 0xf8) < mapper.length ){
			newMapper = new byte[range];
			System.arraycopy(mapper, minIndex - start, newMapper, 0, range);
			newStart = minIndex;
		}
		
		IARTNodeX[] newNodes = new IARTNodeX[count];
		
		for (int i = 0; i < newNodes.length; i++) {
			newNodes[i] = nodes[i].compact();
		}
		
		
		
		
		
		return new ARTNode2NbC(newStart, newMapper, newNodes);
	}


}
