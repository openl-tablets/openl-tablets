package org.openl.util.trie;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

public class TreeTest {

	@Test
	public void test() {
		testOp(new ARTree1<ISequentialKey, Integer>());
		testOp(new ARTree2<ISequentialKey, Integer>());
	}

	private void testOp(IARTree<ISequentialKey, Integer> tree) {
		
		String[] keys = {"AAA", "AAB", "AAD", "BCA", "BC", "AA", "AB"};
		
		for (int i = 0; i < keys.length; i++) {
			tree.put(new CharSequenceKey(keys[i]), i);
		}
		
		tree.compact();
		
		for (int i = 0; i < keys.length; i++) {
			assertEquals(i, (int)(Integer) tree.get(new CharSequenceKey(keys[i])));
		}
		
		
		Iterator<IARTNode> it = tree.nodeIteratorDepthFirst();
		
		while (it.hasNext()) {
			IARTNode node =  it.next();
			System.out.println("N=" + node.countN() + " V= " + node.countV() + "  " + node.getClass().getName());
			
		}
		
		
	}

}
