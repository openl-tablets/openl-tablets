package org.openl.util.ce;

public interface IServiceMTConfiguration {

	/**
	 * 
	 * @return target parallelism level. Should return values from 1 to the number of available cores
	 */
	
	int getParallelLevel();
	
	
	/**
	 * 
	 * @return maximum number of errors that must be caught before (optionally) breaking the parallel array execution 
	 */
	
	int getErrorLimit();

	
	/**
	 * 
	 * @return minimum sequence length in nano-seconds. Below this value it is not recommended 
	 * to split the work as overhead and synchronization costs may consume the possible parallel performance improvement
	 */
	
	long getMinSequenceLengthNs(); 
	
	
}
