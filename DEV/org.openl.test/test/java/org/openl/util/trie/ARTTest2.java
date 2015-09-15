package org.openl.util.trie;

import java.util.Iterator;

public class ARTTest2 extends MemTest {


	@SuppressWarnings("unchecked")
	@Override
	protected ARTree2<Integer> doSomething(String[] data) {
		ARTree2<Integer> tree = new ARTree2<Integer>();

		for (int i = 0; i < data.length; i++) {
			tree.put(new CharSequenceKey(new String(data[i])), i);
		}
		tree.compact();

		return tree;

	}
	
	
	protected void doSomething2(String[] data, Object x) {
		@SuppressWarnings("unchecked")
		ARTree2<Integer> tree = (ARTree2<Integer>)x;

		for (int i = 0; i < data.length; i++) {
			int res = tree.get(new CharSequenceKey(new String(data[i])));
			if (res != i)
				throw new RuntimeException(res + " != " + i);
		}
		
	}


	public static void main(String[] args) {
		boolean notProfiling = true;
		if (args.length > 0 && args[0].equals("f"))
			notProfiling = false;
		
		String[] data = loadData();
//		new ARTTest2().run3(ARTree2.class, 1, notProfiling, data);
		showStats(data);
	}

	
	static void showStats(String[] data)
	{
		ARTree2<Integer> tree = new ARTree2<Integer>();

		for (int i = 0; i < data.length; i++) {
			tree.put(new CharSequenceKey(new String(data[i])), i);
		}
		
		tree.compact();
		
		int[][] ary = new int[128][128];
		int count = 0;
		
		Iterator<IARTNode> it = tree.nodeIteratorDepthFirst();
		for(;it.hasNext();)
		{
			IARTNode node = it.next();
			++count;
			++ary[node.countN()][node.countV()];
		}
		
		System.out.println("Count = " + count);
		
		for (int i = 0; i < 128; i++) {
			for (int j = 0; j < 128; j++) {
				if (ary[i][j] != 0)
				{
					System.out.println(i + "N x " + j + "V : " + ary[i][j]);
				}	
			}
		}
		
		
		
	}
	
}
