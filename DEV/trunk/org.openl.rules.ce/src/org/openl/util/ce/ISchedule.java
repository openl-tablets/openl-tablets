package org.openl.util.ce;

public interface ISchedule {
	
	
	/**
	 * Prepares a set of {@link Runnable} tasks to run in parallel order. 
	 * Optimizations should be made to minimize overhead of this method as much as possible. 
	 * All expensive operations must be done at the initial Schedule preparation time 
	 * @return
	 */
	
	Runnable[] getTasks();


}
