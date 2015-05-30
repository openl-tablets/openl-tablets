package org.openl.util.trie;

import org.openl.util.trie.ISequentialKey.KeyRange;
import org.openl.util.trie.nodes.ARTNode1NI;

public final class ARTree1<K extends ISequentialKey, V> implements IARTree<K , V> {
	
	
	IARTNodeX root;
	
	
	public ARTree1()
	{
		this(defaultRange());
	}
	

	public ARTree1(KeyRange rootRange) {
		this.root = createNode(rootRange);
	}


	private IARTNodeX createNode(KeyRange range) {
		int start = range.initialMin();
		int capacity = range.initialMax() - range.initialMin() + 1;
		
		return new ARTNode1NI(start, capacity);
	}


	private static KeyRange defaultRange() {
		return CharSequenceKey.UTF8RangeKey;
	}


	@Override
	public void put(K key, V value) {
		
		root = insert(root, key, value, 0);
		
	}

	private IARTNodeX insert(IARTNodeX current, K key, V value, int depth) {
		int len = key.length();
		if (depth == len - 1)
			return insertValue((IARTNodeV)current, key, value);
		
		int index = key.keyAt(depth);
		
		IARTNodeN currentN = (IARTNodeN)current;
		
		IARTNodeX next = currentN.findNode(index);
		if (next != null)
		{	
			IARTNodeX next1 = insert(next, key, value, depth + 1);
			if (next1 != next)
				currentN.setNode(index, next1);
			return currentN;
		}
		
		next = createNext(key, depth);
		next = insert(next, key, value, depth + 1);
		currentN = currentN.setNode(index, next);
		
		return currentN;
	}

	private IARTNodeX createNext(K key, int depth) {
		KeyRange range = key.keyRange(depth);
		return createNode(range);
	}
	
	

	private IARTNodeV insertValue(IARTNodeV current, K key, V value) {

		int index = key.keyAt(key.length() - 1);
		
		current = current.setValue(index, value);
		
		return current;
		
	}


	@Override
	public V get(K key) {
		int len = key.length() - 1;
		IARTNodeX current = root;
		for (int depth = 0; depth < len; depth++) {
			int index = key.keyAt(depth);
			
			IARTNodeX next = ((IARTNodeN)current).findNode(index);
			if (next == null)
				return null;
			
			current = next;
			
		}
		
		@SuppressWarnings("unchecked")
		V res = (V)((IARTNodeV)current).getValue(key.keyAt(len));
		return res;
	}


	public void compact() {
		root = root.compact();
	}


}
