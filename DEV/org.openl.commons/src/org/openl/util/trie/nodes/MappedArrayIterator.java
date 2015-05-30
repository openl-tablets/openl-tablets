package org.openl.util.trie.nodes;

import java.util.NoSuchElementException;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;

public class MappedArrayIterator extends AIntIterator {
	
	int start;
	
	private MappedArrayIterator(int start, byte[] data) {
		super();
		this.start = start;
		this.data = data;
		nextPosition(0);
	}

	byte[] data;
	int position;
	
	void nextPosition(int from)
	{
		while(from < data.length && data[from] == 0)
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
	
	static public IIntIterator iterator(int start, byte[] data)
	{
		return data == null ? AIntIterator.fromValue() : new MappedArrayIterator(start, data);
	}
	

}
