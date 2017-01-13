package org.openl.util.trie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.runtime.RulesEngineFactory;

public abstract class MemTest {




	String file;
	int nTimes;
	int nIndexes;
	boolean executionMode;

	public static void main(String[] args) {


		int nTimes = Integer.parseInt(args[0]);

		boolean notProfiling = args.length > 4 ? Boolean.parseBoolean(args[1])
				: true;

		System.out.println("" + nTimes + '\t'
				+ notProfiling);

	}

	Object ary;
	private Object[] ary1;

	protected <T> void run2(Class<T> clazz, int nTimes, boolean notProfiling) {
		
		prepare();

		memcheck();
		
		
		ary = Array.newInstance(clazz, nTimes);
		
		for (int i = 0; i < nTimes; i++) {
			
			Array.set(ary, i, doSomething(null));
		}

		memcheck();

		if (notProfiling)
			if (!ask("Continue?"))
				return;

	}
	
	
	protected <T> void run3(Class<T> clazz, int nTimes, boolean notProfiling, String[] data) {
		
		prepare();
		memcheck();
		
		ary1 = new Object[nTimes];
		
		
		long start = System.nanoTime();
		
		for (int i = 0; i < nTimes; i++) {
			
			ary1[i]=  doSomething(data);
		}
		long end = System.nanoTime();
		
		
		nTimes *= 10;
		for (int i = 0; i < nTimes; i++) {
			
			doSomething2(data, ary1[0]);
		}

		long end2 = System.nanoTime();
		
		System.out.println("1: " + (end-start)/nTimes/100 + "us");
		System.out.println("2: " + (end2-end)/nTimes/1000 + "us");
		memcheck();

		if (notProfiling)
			if (!ask("Continue?"))
				return;
		
		
	}
		

	protected void doSomething2(String[] data, Object object) {
	}


	protected void prepare() {
		
	}

	protected abstract <T> T doSomething(String[] data);

	private boolean ask(String msg) {
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(
				System.in));
		System.out.println(msg);
		String res;
		try {
			res = stdIn.readLine();
			return res.startsWith("y");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	int memIdx;

	static class MemStore {
		long fm, tm, mm;
	}

	List<MemStore> mss = new ArrayList<MemStore>();

	private int logLevel = 1;
	private long start;
	private long end;

	private void memcheck() {
		end = System.nanoTime();

		System.out
				.println("Before gc: \n"
						+ printMemBeans(
								ManagementFactory.getMemoryPoolMXBeans(), false));

		System.gc();
		System.gc();
		System.gc();

		System.out
				.println("After  gc: \n"
						+ printMemBeans(
								ManagementFactory.getMemoryPoolMXBeans(), false));

		MemStore ms = new MemStore();

		ms.fm = Runtime.getRuntime().freeMemory();

		ms.tm = Runtime.getRuntime().totalMemory();
		ms.mm = Runtime.getRuntime().maxMemory();

		mss.add(ms);

		MemStore ms1 = mss.size() > 1 ? mss.get(mss.size() - 2) : ms;

		long oldStart = start == 0 ? end : start;

		if (logLevel > 0)
			System.out.println("Memory:\t" + p(ms.fm) + '\t' + p(ms.tm) + '\t'
					+ p(ms.mm) + '\t' + p(ms.tm - ms.fm) + '\t'
					+ pt(end - oldStart) + "\t||\t "

					+ p(ms.fm - ms1.fm) + '\t' + p(ms.tm - ms1.tm) + '\t'
					+ p(ms.mm - ms1.mm) + '\t'
					+ p(ms.tm - ms.fm - ms1.tm + ms1.fm) + '\t');
		start = System.nanoTime();

	}

	private String printMemBeans(List<MemoryPoolMXBean> list, boolean totalOnly) {

		long init = 0, used = 0, committed = 0, max = 0;
		StringBuilder sb = new StringBuilder();
		for (MemoryPoolMXBean bean : list) {
			MemoryUsage usage = bean.getUsage();
			sb.append(bean.getName()).append("\t").append(printUsage(usage))
					.append("\n");

			init += usage.getInit();
			committed += usage.getCommitted();
			used += usage.getUsed();
			max += usage.getMax();

		}
		MemoryUsage totalUsage = new MemoryUsage(init, used, committed, max);

		sb.append("Total::    ").append("\t").append(printUsage(totalUsage))
				.append("\n");

		return sb.toString();
	}

	private Object printUsage(MemoryUsage usage) {
		return "used = " + p(usage.getUsed()) + '\t' + "commit = "
				+ p(usage.getCommitted());
	}

	static DecimalFormat df = new DecimalFormat("#,###");

	private String p(long mem) {
		return df.format((mem + 1024 * 512) / (1024 * 1024)) + 'M';
	}

	private String pt(long tm) {
		return df.format((tm + 500000) / (1000000)) + "ms";
	}
	
	
	public interface IData
	{
		String[] getCodes();
	}
	
	static String[] loadData()
	{
		
		long start = System.currentTimeMillis();
		System.out.println();
		
		RulesEngineFactory<IData> re = new RulesEngineFactory<IData>("resources/trie-test/ManyStrings.xlsx", IData.class);
		Object e = re.newEngineInstance();
		
		IData ee = (IData)e;
		long end = System.currentTimeMillis();
		String[] dd = ee.getCodes();
				
		System.out.println("Loaded " + dd.length + " in " + (end - start) + " ms");
		return dd; 
	}

}
