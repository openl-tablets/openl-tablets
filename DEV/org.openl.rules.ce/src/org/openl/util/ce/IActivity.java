package org.openl.util.ce;

public interface IActivity extends IDependent<IActivity>{
	
	/**
	 * 
	 * @return estimated duration of the activity in nano-seconds. 
	 * 
	 * It is obvious that different CPUs provide different performance characteristics. 
	 * At this point we assume that all the measurements are made on 2.6 GHz i5-2450M CPU. 
	 *  
	 * 	 
	 * It is important to keep it consistent across the application. We recommend to use exact  nano-second measures where possible 
	 * (as in {@link System#nanoTime()}). It is still 
	 * This way one day we may use actual measurements vs estimates to improve scheduling. For measurements you may use
	 * OpenL {@link Benchmark} framework for it provides more reliable measurements.
	 * 
	 *  It is important to understand that this number will affect the quality of the parallel 
	 *  scheduling and the total efficiency of the algorithm. It is recommended that you use some close-to-real numbers 
	 *  (either available runtime or pre-calculated statically) for estimates. Make them related to the actual size of a problem. 
	 *  For example, if an activity parses a table, one might use <code>width * height</code> as the an estimate factor. 
	 *  
	 *  The following two errors will reduce overall algorithm efficiency:
	 *  If your estimate is too low, the algorithm may want to leave the task non-parallel or use low parallelism.  
	 *  In case of overestimated problem, the scheduling algorithm will use too short sequences producing too much overhead.
	 */
	
	long duration();
	
	

}
