package org.openl.util.trie.nodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.IARTNodeVI;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.cnodes.ARTNode1VI;
import org.openl.util.trie.cnodes.ARTNode2VIbC;
import org.openl.util.trie.cnodes.NodeFactory;
import org.openl.util.trie.cnodes.SingleNodeVI;

public class ARTNode2VIb implements IARTNodeVI {


	private static final int MAXIMUM_CAPACITY = 255;

	int start;
	
	int[] values;
	byte[] mapper;

	private int count = 0;

	private int minIndex;

	private int maxIndex;
	
	
	public ARTNode2VIb(){}
	public ARTNode2VIb(int start, int capacity, int mapCapacity)
	{
		init(start, capacity, mapCapacity);
	}
	
	
	public void init(int start, int capacity, int mapCapacity)
	{
		values = new int[capacity];
		mapper = new byte[mapCapacity];
		this.start = start;
		minIndex = start + mapCapacity;
		maxIndex = start;
	}
	
	
	@Override
	public Object getValue(int index) {
		if (mapper == null)
			return null;
		
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
		
		if (mapper == null)
			init(index);
		
		int idx = index - start;
		
		if (idx < 0) 
			return growNodeDown(index).setValue(index, value);
		
		if (idx >= mapper.length)
			return growMapperUp(index).setValue(index, value);
		
		
		
		if (mapper[idx] == 0)
		{
			if (count  >= values.length){
				return growCapacity().setValue(index, value);
			}
			
			minIndex = Math.min(index, minIndex);
			maxIndex = Math.max(index, maxIndex);
			values[count++] = (Integer)value;
			mapper[idx] = (byte)(256 - count);
		}	
		
		else {
			byte b = mapper[idx];
			values[(255 - b) & 0xff] = (Integer)value;
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


	private IARTNodeV growCapacity() {
		int oldCapacity = values.length;
		int newCapacity = newCapacity(oldCapacity * 2);
		if (newCapacity <= oldCapacity)
			throw new RuntimeException("Node capacity overflow");
		
		int[] newValues = new int[newCapacity];
		System.arraycopy(values, 0, newValues, 0, oldCapacity);
		values = newValues;
		
		return this;
	}


	private IARTNodeV growMapperUp(int index) {
		
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

	private IARTNodeV growNodeDown(int index) {
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
	public int countV() {
		return count ;
	}

	@Override
	public int minIndexV() {
		return minIndex;
	}

	@Override
	public int maxIndexV() {
		return maxIndex;
	}
	@Override
	public IIntIterator indexIteratorV() {
		return MappedArrayIterator.iterator(start, mapper);
	}
	
	@Override
	public IARTNodeX compact() {
		switch (count) {
		case 0:
			return IARTNodeV.EMPTY;
			
		case 1:
			return new SingleNodeVI(minIndex, values[0]);
		}
		
		int range = maxIndex - minIndex + 1;
		boolean useMapper = NodeFactory.useMapper(range,  byte[].class, count, int[].class); 
		
		if (useMapper)
			return makeMappedCompact(range);
		
		
		return makeArrayCompact(range);
	}
	
	private IARTNodeVI makeArrayCompact(int range) {
		
		int[] res = new int[range];
		
		for (int i = minIndex; i <= maxIndex; i++) {
			byte b = mapper[i - start];
			if (b != 0)
			{
				res[i - minIndex] = 0xffffffff -  values[(255 - b) & 0xff];
			}	
			
		}
		return new ARTNode1VI(minIndex, count, res);
	}
	
	public IARTNodeVI makeMappedCompact(int range) {
		
		
		int newStart = start;
		byte[] newMapper = mapper;	
		if (((range + 7) & 0xf8) < mapper.length ){
			newMapper = new byte[range];
			System.arraycopy(mapper, minIndex - start, newMapper, 0, range);
			newStart = minIndex;
		}
		
		int[] newValues = values;	
		if (((count + 1) & 0xfe) < values.length)
		{
			newValues = new int[count];
			System.arraycopy(values, 0, newValues, 0, count);
		}	
		
		return new ARTNode2VIbC(newStart, newMapper, newValues);
	}

	

}
