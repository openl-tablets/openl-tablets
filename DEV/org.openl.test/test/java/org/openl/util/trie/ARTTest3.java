package org.openl.util.trie;

public class ARTTest3 extends MemTest {


	@SuppressWarnings("unchecked")
	@Override
	protected ARTree3<Integer> doSomething(String[] data) {
		ARTree3<Integer> tree = new ARTree3<Integer>();

		for (int i = 0; i < data.length; i++) {
			tree.put(data[i], i);
		}
		tree.compact();

		return tree;

	}
	
	
	protected void doSomething2(String[] data, Object x) {
		@SuppressWarnings("unchecked")
		ARTree3<Integer> tree = (ARTree3<Integer>)x;

		for (int i = 0; i < data.length; i++) {
			int res = tree.get(data[i]);
			if (res != i)
				throw new RuntimeException(res + " != " + i);
		}
		
	}


	public static void main(String[] args) {
		boolean notProfiling = true;
		if (args.length > 0 && args[0].equals("f"))
			notProfiling = false;
		
		new ARTTest3().run3(ARTree3.class, 300, notProfiling, loadData());
	}

	
		
		
	
}
