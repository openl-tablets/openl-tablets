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

		// root = insert(root, key, value, 0);
		int depth = 0;
		IARTNode current = root;

		// }

		// private IARTNodeX insert(IARTNodeX current, K key, V value, int
		// depth) {

		int len = key.length();

		for (;;++depth) {
			int index = key.keyAt(depth);
			if (depth == len - 1)
			{	
				((IARTNodeV)current).setValue(index, value);
//				insertValue( current, key, value);
				return;
			}	



			IARTNode next = current.findNode(index);
			if (next != null) {
				current = next;
				continue;
			}

			next = createNext(key, depth);
			current.setNode(index, next);
			current = next;
		}
	}

	private IARTNode createNext(K key, int depth) {
		KeyRange range = key.keyRange(depth);
		return createNode(range);
	}


	@Override
	public V get(K key) {
		int len = key.length() - 1;
		IARTNodeX current = root;
		for (int depth = 0; depth < len; depth++) {
			int index = key.keyAt(depth);

			IARTNodeX next = ((IARTNodeN) current).findNode(index);
			if (next == null)
				return null;

			current = next;

		}

		@SuppressWarnings("unchecked")
		V res = (V) ((IARTNodeV) current).getValue(key.keyAt(len));
		return res;
	}

	public void compact() {
		root = root.compact();
	}
	
	
	@Override
	public Iterator<IARTNode> nodeIteratorDepthFirst() {
		return new DepthFirstNodeIterator(root);
	}


}
