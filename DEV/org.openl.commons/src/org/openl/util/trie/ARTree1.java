package org.openl.util.trie;

import java.util.Iterator;

import org.openl.util.trie.ISequentialKey.KeyRange;
import org.openl.util.trie.nodes.ARTNode1NbVib;

public final class ARTree1<K extends ISequentialKey, V> implements IARTree<K , V> {
	
	
	IARTNode root;
	
	
	public ARTree1()
	{
		this(defaultRange());
	}
	

	public ARTree1(KeyRange rootRange) {
		this.root = createNode(rootRange);
	}


	private IARTNode createNode(KeyRange range) {
//		int start = range.initialMin();
//		int capacity = range.initialMax() - range.initialMin() + 1;
		
		return new ARTNode1NbVib();
	}


	private static KeyRange defaultRange() {
		return CharSequenceKey.UTF8RangeKey;
	}


	@Override
	public void put(K key, V value) {
		
		root = insert(root, key, value, 0);
		
	}

	private IARTNode insert(IARTNode current, K key, V value, int depth) {
		int len = key.length();
		if (depth == len - 1)
			return insertValue(current, key, value);
		
		int index = key.keyAt(depth);
		
		
		IARTNode next = current.findNode(index);
		if (next != null)
		{	
			IARTNode next1 = insert(next, key, value, depth + 1);
			if (next1 != next)
				current.setNode(index, next1);
			return current;
		}
		
		next = createNext(key, depth);
		next = insert(next, key, value, depth + 1);
		current.setNode(index, next);
		
		return current;
	}

	private IARTNode createNext(K key, int depth) {
		return new ARTNode1NbVib();
		
	}
	
	

	private IARTNode insertValue(IARTNodeV current, K key, V value) {

		int index = key.keyAt(key.length() - 1);
		
		current.setValue(index, value);
		
		return (IARTNode)current;
		
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
		root = (IARTNode) root.compact();
	}


	@Override
	public Iterator<IARTNode> nodeIteratorDepthFirst() {
		return new DepthFirstNodeIterator(root);
	}


}
