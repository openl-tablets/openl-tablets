package org.openl.util.trie;

import java.util.Collections;
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

public final class ARTree3<V> implements IARPrefixTree<V>, IARTree<V>{

	IARTNode root;

	public ARTree3() {
		this(defaultRange());
	}

	public ARTree3(KeyRange rootRange) {
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

	public void put(CharSequence key, V value) {

		
		IARTNode current = root;


		int len = key.length() - 1;

		for (int depth = 0; depth < len; ++depth) {
			int index = key.charAt(depth);

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
		current.setValue(key.charAt(len), value);
	}


	
	

	@SuppressWarnings("unchecked")
	public V get(CharSequence key) {
		int len = key.length() - 1;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {

			current = ((IARTNodeN) current).findNode(key.charAt(depth));
			if (current == null)
				return null;
		}

		return (V)  current.getValue(key.charAt(len));
	}

	public void compact() {
		root = root.compact();
	}
	
	
	public Iterator<IARTNode> nodeIteratorDepthFirst() {
		return new DepthFirstNodeIterator(root);
	}

	@Override
	public void put(ISequentialKey key, V value) {
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

	@SuppressWarnings("unchecked")
	@Override
	public V get(ISequentialKey key) {
		int len = key.length() - 1;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {

			current = ((IARTNodeN) current).findNode(key.keyAt(depth));
			if (current == null)
				return null;
		}

		return (V)  current.getValue(key.keyAt(len));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<V> allPrefixesOf(ISequentialKey key) {
		
		
		
		int len = key.length() - 1;
		if (len < 0)
			//TODO: Use JAVA7 Collections.emptyIterator()
			return Collections.<V>emptyList().iterator();
		
		Object[] ary = new Object[len + 1];
		int maxNode = -1;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {
			int index = key.keyAt(depth);
			V x = (V)current.getValue(index);
			if (x != null)
			{	
				maxNode = depth;
				ary[depth] = x;
			}	
			
			current = ((IARTNodeN) current).findNode(index);
			if (current == null)
				return new ValueArrayIterator<V>(ary, maxNode);
			
		}

		V x = (V)  current.getValue(key.keyAt(len));
		if (x != null)
		{
			ary[len] = x;
			maxNode = len;
			
		}	
		return new ValueArrayIterator<V>(ary, maxNode);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getLongestPrefixValue(ISequentialKey key) {
		int len = key.length() - 1;
		V ret = null;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {
			int index = key.keyAt(depth);
			V x = (V)current.getValue(index);
			if (x != null)
				ret = x;
			
			current = ((IARTNodeN) current).findNode(index);
			if (current == null)
				return ret;
		}

		V x = (V)  current.getValue(key.keyAt(len));
		return x != null ? x : ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getLongestPrefixValue(CharSequence key) {
		int len = key.length() - 1;
		V ret = null;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {
			int index = key.charAt(depth);
			V x = (V)current.getValue(index);
			if (x != null)
				ret = x;
			
			current = ((IARTNodeN) current).findNode(index);
			if (current == null)
				return ret;
		}

		V x = (V)  current.getValue(key.charAt(len));
		return x != null ? x : ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<V> allPrefixesOf(CharSequence key) {
		
		
		
		int len = key.length() - 1;
		if (len < 0)
			//TODO: Use JAVA7 Collections.emptyIterator()
			return Collections.<V>emptyList().iterator();
		
		Object[] ary = new Object[len + 1];
		int maxNode = -1;
		IARTNode current = root;
		for (int depth = 0; depth < len; depth++) {
			int index = key.charAt(depth);
			V x = (V)current.getValue(index);
			if (x != null)
			{	
				maxNode = depth;
				ary[depth] = x;
			}	
			
			current = ((IARTNodeN) current).findNode(index);
			if (current == null)
				return new ValueArrayIterator<V>(ary, maxNode);
			
		}

		V x = (V)  current.getValue(key.charAt(len));
		if (x != null)
		{
			ary[len] = x;
			maxNode = len;
			
		}	
		return new ValueArrayIterator<V>(ary, maxNode);
	}
}
