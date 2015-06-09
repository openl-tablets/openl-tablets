package org.openl.util.trie;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.openl.util.AOpenIterator;

public class PrefixTest {

	@Test
	public void test() {
		ARTree3<Integer> tree =  new ARTree3<Integer>();
		
		tree.put("abcd", 4);
		tree.put("abc", 3);
		tree.put("ab", 2);
		tree.put("abcdmno", 7);
		tree.put("axcxccb", 111);
		
		assertEquals((Integer)3, tree.getLongestPrefixValue("abcxyz"));
		assertEquals((Integer)4, tree.getLongestPrefixValue("abcdxyz"));
		
		assertEquals((Integer)3, tree.getLongestPrefixValue("abc"));
		assertEquals((Integer)2, tree.getLongestPrefixValue("abasdasfd"));
		
		
		Iterator<Integer> it = tree.allPrefixesOf(new CharSequenceKey("abcdmnox"));
		
		List<Integer> list = AOpenIterator.asList(it);
		assertEquals(list.size(), 4);
		assertEquals(Integer.valueOf(7), list.get(0));
		assertEquals(Integer.valueOf(4), list.get(1));
		assertEquals(Integer.valueOf(3), list.get(2));
		assertEquals(Integer.valueOf(2), list.get(3));
	}
	
	

}
