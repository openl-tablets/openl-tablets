package org.openl.util.ce;

import java.util.concurrent.Callable;

import org.openl.binding.impl.MultiCallMethodBoundNode;
import org.openl.util.IConvertor;
import org.openl.util.ce.conf.ServiceMTConfiguration;

/**
 * 
 * This is the central point of everything parallel multi-threaded execution in
 * OpenL MT framework
 * 
 * 
 * The only goal of the parallel framework is to increase performance vs
 * single-threaded execution. It is done by utilizing the number of available
 * CPU cores.
 * 
 * The tricky part comes from the fact that this resource is shared between
 * different applications and threads, not only within a single JVM but also
 * outside of the JVM boundaries. In the real life we may expect different usage
 * scenarios and get different results with the same algorithms. Also, we know
 * that different hardware platforms and different versions of the JVMs may
 * demonstrate different performance characteristics.
 * 
 * All this knowledge regarding the task complexity should not preclude us from
 * attempting to work on parallel execution of the different parts of the OpenL
 * framework. As number of cores in commercial CPUs grows every year, even
 * mediocre algorithms will produce a significant improvement in performance,
 * without being optimal.
 * 
 * At this point we have the following classes of tasks as candidates for
 * parallel execution:
 * 
 * 1) Symmetrical array processing. A subset of embarrassingly parallel problems
 * where each element has approximately the same computational complexity.
 * Examples: <li>Runtime: MultiCall processing: applying the same method to an
 * array of arguments {@link MultiCallMethodBoundNode}. Such as processing array
 * of vehicles within an insurance policy rating algorithm, or processing array
 * of coverages within a single vehicle in the same algorithm <li>Parsing:
 * Processing an array of rows in a table
 * 
 * This case requires a minimal effort to efficiently schedule the execution.
 * The only issue here is defining proper minimal efficient size of the
 * sequential execution.
 * 
 * 2) Asymmetrical array processing. Each element has possibly different
 * complexity, it is still possible to estimate the complexity of the task
 * beforehand Examples: <li>Parsing: worksheets within a workbook <li>Parsing:
 * Tables
 * 
 * The approach here would be to schedule the longest tasks first, this should
 * provide good enough performance characteristics (need to verify)
 * 
 * 3) Tasks with static dependency graphs Examples: <li>Spreadsheets
 * 
 * The approach is to use critical path scheduling and dynamic execution
 * 
 * 4) Tasks with dynamic dependencies Examples: <li>Parsing of included
 * workbooks
 * 
 * 
 * 
 * 
 * The additional goals of the framework are the following:
 * 
 * 1) Provide a single configuration management mechanism for parallel execution
 * of different types of activities 2) Use simple API for different cases 3)
 * Optimize parallel performance using static and dynamic information
 * 
 * In the future we may provide more features to improve scheduling of tasks
 * based on priorities and different SLA requirements, but initially we just
 * want to increase the performance in simple cases, get low-hanging fruits
 * 
 * 
 * @author snshor
 * 
 */

public interface IServiceMT {

	/**
	 * Executes an array of Callable elements, trying to make calculations
	 * parallel. Each of the elements in array should are considered independent
	 * and are of approximately the same computational complexity expressed as a
	 * single duration value (symmetrical)
	 * 
	 * @param calls
	 *            array of {@link Callable} to execute
	 * @param result
	 *            a placeholder for the result
	 * @param durationEstimate
	 *            @see {@link IActivity#duration()}
	 * @throws ArrayExecutionException
	 *             containing all the exceptions during the execution; the
	 *             service may be configured to impose a limit on number of
	 *             captured exceptions and interrupt the execution process
	 *             before all the elements have been processed
	 * 
	 * @return execution time in nanoSeconds as measured in System#nanoTime()
	 */

	<T> long executeAll(Callable<T>[] calls, T[] result, long durationEstimate)
			throws ArrayExecutionException;

	/**
	 * Transforms an array of elements A into an array of results T using
	 * convertor <code>conv</code>.
	 * 
	 * @param conv
	 * @param inputArray
	 * @param result
	 * @param durationEstimate
	 * @return
	 * @throws ArrayExecutionException
	 */

	<A, T> long executeArray(IConvertor<A, T> conv, A[] inputArray, T[] result,
			long durationEstimate) throws ArrayExecutionException;

	/**
	 * Transforms an a sequence of indexes from 0 to result.length-1 into an
	 * array of results T using convertor <code>conv</code>.
	 * 
	 * @param conv
	 * @param result
	 * @param durationEstimate
	 * @return
	 * @throws ArrayExecutionException
	 */

	<T> long executeIndexed(IMTConvertorFactory<T> factory, T[] result)
			throws ArrayExecutionException;

	/**
	 * Transforms an a sequence of indexes from 0 to result.length-1 into an
	 * array of results T using convertor <code>conv</code>.
	 * 
	 * @param conv
	 * @param result
	 * @param durationEstimate
	 * @return
	 * @throws ArrayExecutionException
	 */

	<T> long executeIndexed(IConvertor<Integer, T> conv, T[] result,
			long durationEstimate) throws ArrayExecutionException;

	<T> long executeIndexedPrimitive(IConvertor<Integer, Object> conv,
			Object results, long durationEstimate)
			throws ArrayExecutionException;

	/**
	 * Executes an array of IActivity elements, trying to make calculations
	 * parallel. Activities could be of different durations. Activities may have
	 * different durations and dependencies.
	 * 
	 * @param calls
	 * @return
	 */
	// <T> long executeIndependent(IActivity<T>[] calls, T[] result) throws
	// ArrayExecutionException;
	//
	// <T> long executeDependent(IActivity<T>[] calls, T[] result) throws
	// ArrayExecutionException;
	//
	//
	//
	//
	// <A,T> long executeArray(IConvertor<A,T> exe, A[] inputArray, T[] result,
	// IConvertor<A, Long> timeEstimator) throws ArrayExecutionException;

	/**
	 * 
	 * @return a value from 0 to 1, indicating a relative load on the CPU. At
	 *         this moment we expect the value to be an heuristic, not an exact
	 *         measurement
	 */

	int getActiveThreadCounter();

	void shutdown();

	long executeAll(Runnable[] tasks) throws ArrayExecutionException;


	IScheduler getScheduler(long singleCellLength);
	
	
	ServiceMTConfiguration getConfig();


}
