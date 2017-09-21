package org.openl.util.trie;

import java.util.HashMap;
import java.util.Map;

public class HashTest1 extends MemTest {

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Integer> doSomething(String[] data) {
	       Map<String, Integer> tree = new HashMap<String, Integer>();
	       
	       for (int i = 0; i < data.length; i++) {
		        tree.put(new String( data[i]), i);
	       }
	       

	        
	        return tree;

	}

	
	protected void doSomething2(String[] data, Object x) {
	       @SuppressWarnings("unchecked")
		   Map<String, Integer> tree = (HashMap<String, Integer>)x;
	       
	       for (int i = 0; i < data.length; i++) {
				int res = tree.get(new String(data[i]));
				if (res != i)
					throw new RuntimeException(res + " != " + i);
	       }
	       
	}
	
	
	public static void main(String[] args) {
//		System.out.println(System.getProperties());
		boolean notProfiling = true;
		if (args.length > 0 && args[0].equals("f"))
			notProfiling = false;
		
	    String[] data = loadData();
		new HashTest1().run3(Map.class, 300, notProfiling, data);
		
	}

}
