package org.openl.tablets.tutorial5;

import java.util.List;

import org.openl.base.INamedThing;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.types.impl.MethodsHelper;
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
    
    public static RuleEngineFactory<Tutorial_5RulesInterface> engineFactory; 
	//Creates new instance of Java Interface for our lesson
	public static Tutorial_5RulesInterface tut5;
	
	static {
	    engineFactory = new RuleEngineFactory<Tutorial_5RulesInterface>(Tutorial_5RulesInterface.__src, 
	            Tutorial_5RulesInterface.class);
	    tut5 = engineFactory.makeInstance();
	}
	
	public static void main(String[] args) throws Exception {

		System.out.println("This program runs a set of Benchmarks.\n" + 
				"Each Benchmark takes about 3-4 sec. \n" +
				"It demonstrates an absolute and relative performance of\n" +
				"the identical tests created for regular and indexed versions of the same Decision Table\n\n" +
				"For interactive Benchmarks and other advanced project \n" + 
				"management tools use OpenL Tablets Webstudio "
				);
				
		List<IOpenMethod> methods = engineFactory.getCompiledOpenClass().getOpenClass().getMethods();
		
		IOpenMethod m = MethodsHelper.getSingleMethod("ampmTo24TestTestAll", methods);
		benchmarkMethod(m);
		
		m = MethodsHelper.getSingleMethod("ampmTo24Ind1TestTestAll", methods);
		benchmarkMethod(m);
		
		m = MethodsHelper.getSingleMethod("ampmTo24Ind2TestTestAll", methods);
		benchmarkMethod(m);
		
		m = MethodsHelper.getSingleMethod("driverPremiumTestTestAll", methods);
		benchmarkMethod(m);
		
		m = MethodsHelper.getSingleMethod("driverPremiumIndTestTestAll", methods);
		benchmarkMethod(m);

		m = MethodsHelper.getSingleMethod("driverPremiumIndTestTestAll", methods);
		benchmarkMethod(m);
		
		m = MethodsHelper.getSingleMethod("largeTableTestTestAll", methods);
		benchmarkMethod(m);

		m = MethodsHelper.getSingleMethod("largeTableTestTestAll", methods);
		benchmarkMethod(m);	
	}
	
	static void benchmarkMethod(IOpenMethod m) throws Exception {
		System.out.println("\n=======================\nBenchmarking " + m.getDisplayName(INamedThing.LONG));
		BenchmarkInfo bi = benchmarkMethod(m, 3000);
		System.out.println(bi);
		System.out.println("Per Unit Tested: " + bi.runsunitsec() + " units/sec");
	}
	
	static public BenchmarkInfo benchmarkMethod(final IOpenMethod m, int ms) throws Exception {
	    final IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
	    final Object target = engineFactory.getOpenClass().newInstance(env);

		final Object[] params = {};
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
			else {
				bu = new BenchmarkUnit() {

					protected void run() throws Exception {
						m.invoke(target, params, env);
					}};
			}
			
			BenchmarkUnit[] buu = {bu};
			BenchmarkInfo bi = new Benchmark(buu).runUnit(bu, ms, false);
			return bi;
		} catch (Throwable t) {
			Log.error("Run Error:", t);
			return new BenchmarkInfo(t, bu, bu.getName());
		}
	}
}
