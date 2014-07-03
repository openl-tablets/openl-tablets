package org.openl.util.ce.impl;

import java.lang.reflect.Array;
import java.util.concurrent.Callable;

import org.openl.util.IConvertor;
import org.openl.util.ce.ArrayExecutionException;
import org.openl.util.ce.IMTConvertorFactory;
import org.openl.util.ce.IScheduler;
import org.openl.util.ce.conf.ServiceMTConfiguration;

/**
 * 
 * Single-threaded implementation of the service. 
 * Should be used in the circumstances when multi-threading is
 * not useful, not needed or even non-desirable.
 * 
 *   <li> multi-threading is not useful when there is none or little multi-core CPU power available
 *   to the application. This could be the case on single or double-core CPUs and other special cases
 *   
 *   <li> multi-threading is not needed, for example, when you run benchmarks and profile linear code performance 
 *   
 *   <li> sometimes multi-threading must be turned off when you run into race conditions or other problems 
 *   that can not be explained from the single-treaded point of view. While we will make sure that OpenL code and 
 *   the code of the libraries that we use pass the most rigorous testing, before releasing it, there is always 
 *   a chance that some things remain unaccounted for, especially in MT, where there is no clear definition of the test 
 *   coverage. And, my friends, there is always your code that may interact with MT in some unexpected ways. 
 *      
 * 
 * @author snshor
 *
 */

public class ServiceST extends ServiceBase {

	

	public ServiceST(ServiceMTConfiguration config) {
		super(config);
	}




	@Override
	public <T> long executeAll(Callable<T>[] calls, T[] result, long duration)
			throws ArrayExecutionException {


		return executeAllSequential(calls, result, 0, result.length);
	}




	@Override
	public <A, T> long executeArray(IConvertor<A, T> conv, A[] inputArray,
			T[] result, long duration) throws ArrayExecutionException {

		return executeArraySequential(conv, inputArray, result, 0, result.length);
	}


	@Override
	public <T> long executeIndexed(IConvertor<Integer, T> conv, T[] result,
			long durationEstimate) throws ArrayExecutionException {
		long time = executeIndexedSequential(conv, result, 0, result.length);
		return time;
	}







	@Override
	public <T> long executeIndexedPrimitive(IConvertor<Integer, Object> conv,
			Object results, long durationEstimate)
			throws ArrayExecutionException {
		return executeIndexedSequentialPrimitive(conv, results, 0, Array.getLength(results));
	}




	@Override
	public <T> long executeIndexed(IMTConvertorFactory<T> factory, T[] result)
			throws ArrayExecutionException {
		
		
		return executeIndexedSequential(factory.makeConvertorInstance(), result, 0, result.length);
	}




	@Override
	public void shutdown() {
		// nothing to do
		
	}




	@Override
	public long executeAll(Runnable[] tasks) throws ArrayExecutionException {
		return executeAllSequential(tasks, 0, tasks.length);
	}




	@Override
	public IScheduler getScheduler(long singleCellLength) {
		return new Scheduler(config, singleCellLength);
	}




	@Override
	public int getActiveThreadCounter() {
		return 0;
	}




	
}
