package org.openl.util.trie;

public class ARTTest1 extends MemTest {


	@SuppressWarnings("unchecked")
	@Override
	protected ARTree1<Integer> doSomething(String[] data) {
		ARTree1<Integer> tree = new ARTree1<Integer>();

		for (int i = 0; i < data.length; i++) {
			tree.put(new CharSequenceKey(new String(data[i])), i);
		}
		tree.compact();

		return tree;

	}
	
	
	protected void doSomething2(String[] data, Object x) {
		@SuppressWarnings("unchecked")
		ARTree1<Integer> tree = (ARTree1<Integer>)x;

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
		
		new ARTTest1().run3(ARTree1.class, 100, notProfiling, loadData());
	}

}
