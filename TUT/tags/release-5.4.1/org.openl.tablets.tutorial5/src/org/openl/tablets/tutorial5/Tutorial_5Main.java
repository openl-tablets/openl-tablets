package org.openl.tablets.tutorial5;

import org.openl.base.INamedThing;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.util.Log;
import org.openl.util.benchmark.Benchmark;
import org.openl.util.benchmark.BenchmarkInfo;
import org.openl.util.benchmark.BenchmarkUnit;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;




/**
 *  OpenL Tablets (c)  2006,2007
 *  https://sourceforge.net/projects/openl-tablets/ 
 */

/**
 * @author snshor
 *
 */






public class Tutorial_5Main {

	//Creates new instance of Java Wrapper for our lesson
	static Tutorial_5Wrapper tut5 = new Tutorial_5Wrapper();

	
	
	public static void main(String[] args) throws Exception {

		System.out.println("This program runs a set of Benchmarks.\n" + 
				"Each Benchmark takes about 3-4 sec. \n" +
				"It demonstrates an absolute and relative performance of\n" +
				"the identical tests created for regular and indexed versions of the same Decision Table\n\n" +
				"For interactive Benchmarks and other advanced project \n" + 
				"management tools use OpenL Tablets Webstudio "
				);
				
		
		IOpenMethod m = Tutorial_5Wrapper.ampmTo24TestTestAll_Method;
		benchmarkMethod(m);
		
		m = Tutorial_5Wrapper.ampmTo24Ind1TestTestAll_Method;
		benchmarkMethod(m);

		m = Tutorial_5Wrapper.ampmTo24Ind2TestTestAll_Method;
		benchmarkMethod(m);

		m = Tutorial_5Wrapper.driverPremiumTestTestAll_Method;
		benchmarkMethod(m);

		m = Tutorial_5Wrapper.driverPremiumIndTestTestAll_Method;
		benchmarkMethod(m);

		m = Tutorial_5Wrapper.driverPremiumIndTestTestAll_Method;
		benchmarkMethod(m);

		m = Tutorial_5Wrapper.largeTableTestTestAll_Method;
		benchmarkMethod(m);

		m = Tutorial_5Wrapper.largeTableIndTestTestAll_Method;
		benchmarkMethod(m);

	
	
	}
	
	
	static void benchmarkMethod(IOpenMethod m) throws Exception
	{
		System.out.println("\n=======================\nBenchmarking " + m.getDisplayName(INamedThing.LONG));
		BenchmarkInfo bi = benchmarkMethod(m, 3000);
		System.out.println(bi);
	}
	
	
	
	
	static public BenchmarkInfo benchmarkMethod(final IOpenMethod m, int ms) throws Exception
	{

		final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
		final Object target = tut5.getOpenClass().newInstance(env);



		final Object[] params = {};
		
//		Object res = null;
		BenchmarkUnit bu = null;

		try
		{
			
			
			if (m instanceof IBenchmarkableMethod)
			{
				final IBenchmarkableMethod bm = (IBenchmarkableMethod)m;
				bu = new BenchmarkUnit()
				{
					protected void run() throws Exception
					{
						throw new RuntimeException();
					}

					public void runNtimes(int times) throws Exception
					{
						bm.invokeBenchmark(target, params, env, times);
					}

					public String getName()
					{
						return bm.getBenchmarkName();
					}

					public int nUnitRuns()
					{
						return bm.nUnitRuns();
					}

					public String[] unitName()
					{
						return bm.unitName();
					}
					
				};
				
			}
			else 
			{
				bu = new BenchmarkUnit()
				{

					protected void run() throws Exception
					{
						m.invoke(target, params, env);
					}};
					
			}
			
			BenchmarkUnit[] buu = {bu};
			BenchmarkInfo bi = new Benchmark(buu).runUnit(bu, ms, false);
//			System.out.print(bi);
			return bi;
			
		} catch (Throwable t)
		{
			Log.error("Run Error:", t);
			return new BenchmarkInfo(t, bu, bu.getName());
		}

	}
	
	

}
