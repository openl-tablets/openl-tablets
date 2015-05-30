package org.openl.util.trie.nodes;

import org.openl.domain.IIntIterator;
import org.openl.util.trie.IARTNode;
import org.openl.util.trie.IARTNodeN;
import org.openl.util.trie.IARTNodeV;
import org.openl.util.trie.IARTNodeVI;
import org.openl.util.trie.IARTNodeX;

public class ARTNode1NI implements IARTNode, IARTNodeVI {

	IARTNodeN nodeN;
	IARTNodeV nodeV;
	
	

	public ARTNode1NI(int start, int capacity) {
//		nodeN = new ARTNode1N(start, capacity);
//		nodeV = new ARTNode1I(start, capacity);
		nodeN = new ARTNode2Nb();
		nodeV = new ARTNode2VIb();
	}



	public IARTNodeX findNode(int index) {
		return nodeN.findNode(index);
	}



	public IARTNodeN setNode(int index, IARTNodeX node) {
		nodeN = nodeN.setNode(index, node);
		return this;
	}



	public int countN() {
		return nodeN.countN();
	}



	public int minIndexN() {
		return nodeN.minIndexN();
	}



	public int maxIndexN() {
		return nodeN.maxIndexN();
	}



	public Object getValue(int index) {
		return nodeV.getValue(index);
	}



	public IARTNodeV setValue(int index, Object value) {
		nodeV = nodeV.setValue(index, value);
		return this;
	}



	public int countV() {
		return nodeV.countV();
	}



	public int minIndexV() {
		return nodeV.minIndexV();
	}



	public int maxIndexV() {
		return nodeV.maxIndexV();
	}



	@Override
	public IIntIterator indexIteratorN() {
		return nodeN.indexIteratorN();
	}



	@Override
	public IIntIterator indexIteratorV() {
		return nodeV.indexIteratorV();
	}



	@Override
	public IARTNodeX compact() {
		if (nodeV.countV() == 0)
			return nodeN.compact();
		if (nodeN.countN() == 0)
			return nodeV.compact();
		nodeV = (IARTNodeV) nodeV.compact();
		nodeN = (IARTNodeN) nodeN.compact();
		return this;
	}
}
