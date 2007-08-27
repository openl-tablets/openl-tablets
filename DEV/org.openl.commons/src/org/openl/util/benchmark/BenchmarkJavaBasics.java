/**
 * Created Jul 7, 2007
 */
package org.openl.util.benchmark;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openl.util.IdMap;
import org.openl.util.IdObject;

/**
 * @author snshor
 *
 */
public class BenchmarkJavaBasics
{

	
	static ThreadLocal tracer = new ThreadLocal();
	
	static public boolean isTracerOn()
	{
		return tracer.get() != null;
	}
	
	
	
	static class Invoke extends BenchmarkUnit
	{
		Invoke() throws Exception
		{}
		
		Method m = BenchmarkJavaBasics.class.getMethod("add", new Class[]{int.class, int.class}); 
		Object[] params = {new Integer(10), new Integer(15)};
		Object bjb = new BenchmarkJavaBasics();
		
		protected void run() throws Exception
		{
			m.invoke(bjb, params);
		}
	}
	
	static class Call extends BenchmarkUnit
	{
		Call() throws Exception
		{}
		
		BenchmarkJavaBasics bjb = new BenchmarkJavaBasics();
		
		protected void run() throws Exception
		{
			bjb.add(10, 15);
		}
	}
	

	static class MapGet extends BenchmarkUnit
	{
		MapGet() throws Exception
		{
			map = new HashMap();
			for(int i = 0; i < 100; ++i)
			  map.put("Xyz" + i, "Xyz" + i);
		}
		
		HashMap map;
		
		String key = "Xyz" + 37;
		
		protected void run() throws Exception
		{
			map.get(key);
		}
	}

	
	static class TreeMapGet extends BenchmarkUnit
	{
		TreeMapGet() throws Exception
		{
			map = new TreeMap();
			for(int i = 0; i < 100; ++i)
			  map.put(new Integer(i *2), "Xyz" + i);
		}
		
		TreeMap map;
		
		Integer key = new Integer(50);
		
		protected void run() throws Exception
		{
			map.get(key);
		}
	}


	static class BSearch extends BenchmarkUnit
	{
		
		static int N = 100;
		BSearch() throws Exception
		{
			buf = new int[N];
			for(int i = 0; i < N; ++i)
			  buf[i] = i * 4;
		}
		
		int[] buf;;
		
		int key = 51;
		
		protected void run() throws Exception
		{
			Arrays.binarySearch(buf, key);
		}
		
	}
	
	static class TreeMapGetFirstKey extends BenchmarkUnit
	{
		TreeMapGetFirstKey() throws Exception
		{
			map = new TreeMap();
			for(int i = 0; i < 100; ++i)
			  map.put(new Integer(i *2), "Xyz" + i);
		}
		
		TreeMap map;
		
		Integer key = new Integer(51);
		
		protected void run() throws Exception
		{
			Iterator it = map.tailMap(key).values().iterator();
			if (it.hasNext())
				it.next();
		}
	}
	

	static class MapInternGet extends BenchmarkUnit
	{
		MapInternGet() throws Exception
		{
			map = new HashMap();
			for(int i = 0; i < 100; ++i)
			  map.put(("Xyz" + i).intern(), "Xyz" + i);
		}
		
		HashMap map;
		
		String key = ("Xyz" + 37).intern();
		
		protected void run() throws Exception
		{
			map.get(key);
		}
	}
	
	
	static class Empty extends BenchmarkUnit
	{
		protected void run() throws Exception
		{
		}
	}
	
	
	static class IDMapGet extends BenchmarkUnit
	{
		IDMapGet() throws Exception
		{
			map = new IdMap(107);
			for(int i = 0; i < 100; ++i)
			  map.add(new IdObject( ("Xyz" + i).hashCode()));
		}
		
		IdMap map;
		
		int key = ("Xyz" + 37).hashCode();
		
		protected void run() throws Exception
		{
			map.get(key);
		}
	}
	

	
	static class ThreadG extends BenchmarkUnit
	{
		protected void run() throws Exception
		{
			BenchmarkJavaBasics.isTracerOn();
		}
	}

	
	
	public static void main(String[] args) throws Exception
	{
		BenchmarkUnit[] bu = {new Empty(), new Call(), new Invoke(), new MapGet(),
				new MapInternGet(),
				new IDMapGet(), new ThreadG(), new TreeMapGet(),
				new TreeMapGetFirstKey(),
				new BSearch()};
		
		Map res = new Benchmark(bu).measureAll(1000);
		
		
		for (Iterator iter = res.entrySet().iterator(); iter.hasNext();)
		{
			Map.Entry element = (Map.Entry) iter.next();
			
			System.out.println(element.getKey() + " = \t" + element.getValue());
			
		}
	}
	
	
	
	
	public int add(int x, int y)
	{
		return x + y;
	}
	
	
}
