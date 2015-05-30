package org.openl.util.trie.nodes;

import java.util.NoSuchElementException;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;

public class ObjectArrayIterator extends AIntIterator {
	
	int start;
	Object[] data;
	int position;
	
	private ObjectArrayIterator(int start, Object[] data) {
		super();
		this.start = start;
		this.data = data;
		nextPosition(0);
	}

	
	void nextPosition(int from)
	{
		while(from < data.length && data[from] == null)
		{
			++from;
		}
		position = from;
	}

	@Override
	public int nextInt() {
		if ( position >= data.length)
			throw new NoSuchElementException();
		int res = start + position;
		nextPosition(position + 1);
		return res;
	}

	@Override
	public boolean isResetable() {
		return true;
	}

	@Override
	public void reset() {
		nextPosition(0);
	}

	@Override
	public boolean hasNext() {
		return position < data.length;
	}
	
	static public IIntIterator iterator(int start, Object[] data)
	{
		return data == null ? AIntIterator.fromValue() : new ObjectArrayIterator(start, data);
	}

}
