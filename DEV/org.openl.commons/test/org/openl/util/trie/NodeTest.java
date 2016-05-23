package org.openl.util.trie;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Random;

import org.junit.Test;
import org.openl.util.trie.cnodes.ARTNode1N;
import org.openl.util.trie.nodes.ARTNode1NbVib;

public class NodeTest {

	
	
	@Test
	public void test1()
	{
		Random r = new Random();
		testGrow(new ARTNode1NbVib());
		testN(new ARTNode1NbVib(), 255, r);
		testV(new ARTNode1NbVib(), 255, r);
	}
	
	
	
	private void testGrow(IARTNode n1) {
		n1.setNode(50, n1);
		n1.setNode(30, n1);
		n1.setNode(79, n1);
		
		assertEquals(3, n1.countN());
		
		assertNotNull(n1.findNode(50));
		assertNotNull(n1.findNode(30));
		assertNotNull(n1.findNode(79));
		
		assertNull(n1.findNode(51));
		
		
		Iterator it = n1.indexIteratorN();
		assertEquals(30, it.next());
		assertEquals(50, it.next());
		assertEquals(79, it.next());
		assertFalse(it.hasNext());

		
	}



	void testN(IARTNodeN n1, int max, Random r)
	{
		
		
		IARTNode[] nodes = new IARTNode[max];
		
		
		
		
		for (int i = 0; i < max * 10; i++) {
			
			int nextIdx = r.nextInt(max);
			if (nodes[nextIdx] == null)
			{
				int count = n1.countN();
				nodes[nextIdx] = new ARTNode1N(0, 10, null);
				
				assertNull(n1.findNode(nextIdx));
				n1.setNode(nextIdx, nodes[nextIdx]);
				assertEquals (count + 1, n1.countN());
			}
			
			assertEquals(nodes[nextIdx], n1.findNode(nextIdx));
		}
		
		System.out.println("Count = " + n1.countN());
		
		
		
	}
	
	void testV(IARTNodeV n1, int max, Random r)
	{
		
		
		Integer[] values = new Integer[max];
		
		
		
		for (int i = 0; i < max * 10; i++) {
			
			int nextIdx = r.nextInt(max);
			if (values[nextIdx] == null)
			{
				int count = n1.countV();
				values[nextIdx] = new Integer(r.nextInt());
				
				assertNull(n1.getValue(nextIdx));
				n1.setValue(nextIdx, values[nextIdx]);
				assertEquals (count + 1, n1.countV());
			}
			
			assertEquals(values[nextIdx], n1.getValue(nextIdx));
		}
		
		System.out.println("CountV = " + n1.countV());
		
		
		
	}
	
}
