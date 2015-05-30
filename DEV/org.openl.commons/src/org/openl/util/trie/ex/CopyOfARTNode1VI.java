package org.openl.util.trie.ex;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.IARTNodeVI;
import org.openl.util.trie.IARTNodeX;
import org.openl.util.trie.nodes.IntArrayIterator;

public class CopyOfARTNode1VI implements IARTNodeVI {

	private static final int MAXIMUM_CAPACITY = Character.MAX_VALUE + 1;

	private static final int MAGIC_VALUE = 0xffffffff;
	
	int start;

	int[] values;

	private int count = 0;

	private int minIndex;

	private int maxIndex;

	public CopyOfARTNode1VI(int start, int capacity) {
		values = new int[capacity];
		this.start = start;
		minIndex = start + capacity;
		maxIndex = start;
	}


	@Override
	public Object getValue(int index) {
		int idx = index - start;
		if (idx < 0 || idx >= values.length)
			return null;
		return values[idx] == 0 ? null : MAGIC_VALUE - values[idx];
	}


	private IARTNodeV growNodeUp(int idx) {

		int[] newValues = new int[newCapacity(idx + 1)];
		System.arraycopy(values, 0, newValues, 0, values.length);
		values = newValues;

		return this;
	}

	private IARTNodeV growNodeDown(int idx) {
		int[] newValues = new int[newCapacity(values.length - idx + 1)];
		System.arraycopy(values, 0, newValues, -idx, values.length);
		start -= idx;
		values = newValues;

		return this;
	}

	public int newCapacity(int required) {

		if (required < 0)
			throw new IllegalArgumentException("Illegal initial capacity: "
					+ required);
		if (required > MAXIMUM_CAPACITY)
			required = MAXIMUM_CAPACITY;

		// Find a power of 2 >= initialCapacity
		int capacity = 1;
		while (capacity < required)
			capacity <<= 1;

		return capacity;

	}

	@Override
	public IARTNodeV setValue(int index, Object value) {
		if (value == null)
			throw new NullPointerException();
		int ivalue = (Integer)value;
		if (ivalue == MAGIC_VALUE)
			throw new IllegalArgumentException();
		
		int idx = index - start;

		if (idx < 0)
			return growNodeDown(idx).setValue(index, value);

		if (idx >= values.length)
			return growNodeUp(idx).setValue(index, value);

		if (values[idx] == 0)
		{	
			++count;
			minIndex = Math.min(index, minIndex);
			maxIndex = Math.max(index, maxIndex);
		}	

		values[idx] = MAGIC_VALUE - ivalue;

		return this;
	}

	@Override
	public int countV() {
		return count;
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

		return IntArrayIterator.iterator(start, values);
	}


	@Override
	public IARTNodeX compact() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
