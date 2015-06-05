package org.openl.util.trie;

import java.util.Iterator;

import org.openl.util.trie.ISequentialKey.KeyRange;
import org.openl.util.trie.nodes.ARTNode1NbVib;

/**
 * 
 * @author snshor
 * 
 * @version 1.0
 * 
 */

public final class ARTree2<K extends ISequentialKey, V> implements
		IARTree<K, V> {

	IARTNode root;

	public ARTree2() {
		this(defaultRange());
	}

	public ARTree2(KeyRange rootRange) {
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

		
		IARTNode current = root;


		int len = key.length() - 1;

		for (int depth = 0; depth < len; ++depth) {
			int index = key.keyAt(depth);

			IARTNode next = current.findNode(index);
			if (next != null) {
				current = next;
			}
			else
			{	
				next = new ARTNode1NbVib();
				current.setNode(index, next);
				current = next;
			}	
			
		}
		current.setValue(key.keyAt(len), value);
	}

	protected IARTNode createNext(K key, int depth) {
		KeyRange range = key.keyRange(depth);
		return createNode(range);
	}

	
	

	@SuppressWarnings("unchecked")
	@Override
	public V get(K key) {
		int len = key.length() - 1;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {

			current = ((IARTNodeN) current).findNode(key.keyAt(depth));
			if (current == null)
				return null;
		}

		return (V)  current.getValue(key.keyAt(len));
	}

	public void compact() {
		root = root.compact();
	}
	
	
	@Override
	public Iterator<IARTNode> nodeIteratorDepthFirst() {
		return new DepthFirstNodeIterator(root);
	}


}
