package org.openl.util.trie;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.openl.domain.IIntIterator;

public class DepthFirstNodeIterator implements Iterator<IARTNode> {

	Stack<IARTNode> nodeStack = new Stack<IARTNode>();
	Stack<IIntIterator> iteratorStackN = new Stack<IIntIterator>();

	IARTNode current;
	IIntIterator iterator;

	public DepthFirstNodeIterator(IARTNode root) {
		current = root;
		iterator = root.indexIteratorN();
	}

	@Override
	public void remove() {
		  throw new UnsupportedOperationException("remove");		
	}

	@Override
	public boolean hasNext() {
		return current != null;
	}

	@Override
	public IARTNode next() {

		if (current == null)
			throw new NoSuchElementException();
		IARTNode res = current;
		findNext();
		return res;
	}

	private void findNext() {

		if (iterator.hasNext()) {
			int index = iterator.nextInt();
			IARTNode next = current.findNode(index);
			nodeStack.push(current);
			iteratorStackN.push(iterator);
			current = next;
			iterator = next.indexIteratorN();
			return;
		}	

		
		
		while (!nodeStack.isEmpty()) {

			current = nodeStack.pop();
			iterator = iteratorStackN.pop();

			if (iterator.hasNext()) {
				int index = iterator.nextInt();
				IARTNode next = current.findNode(index);
				nodeStack.push(current);
				iteratorStackN.push(iterator);
				current = next;
				iterator = next.indexIteratorN();
				return;
			}
		}
		
		current = null;
		
		

	}

}
