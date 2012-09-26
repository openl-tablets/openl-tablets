package org.openl.util.ce.conf;

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
	

	
	
	
	
	/**
	 * Returns true, if a particular component should be called in multi-threaded mode. 
	 * This is an indicator for a caller to use MT when the method with <code>name == uid</code> is called using Array Arguments call 
	 *  
	 * @param uid - unique id, such as method name etc.
	 * @return
	 */
	boolean isCallComponentUsingMT(String uid);
	
	
	void setCallComponentUsingMT(String uid, boolean isComponentUsingMT);
	

	/**
	 * Returns true, if a particular component's <b>internals</b> should be executed in the multi-threaded mode. 
	 * This is mostly related to Spreadsheet component
	 *  
	 * @param uid - unique id, such as method name etc.
	 * @return
	 */
	boolean isExecuteComponentUsingMT(String uid);
	
	
	void setExecuteComponentUsingMT(String uid, boolean isComponentUsingMT);
	
	/**
	 * Returns sequential execution length in  nano-seconds for a component defined by the uid. 
	 * The values are either set-up by developers, or are calculated automatically during the execution 
	 * through performance statistics gathering component  
	 * @param uid
	 * @return
	 */
	long getComponentLengthNs(String id);

	void setComponentLengthNs(String uid, long length);


	/**
	 * This method is called when the length of the component is not known, but a developer wants to run the component in parallel. Any value that is more than getMinSequenceLengthNs
	 * will provide the maximum parallelism. The values that are less than getMinSequenceLengthNs will provide proportional decrease in parallelism
	 * 
	 * @see #getMinSequenceLengthNs
	 *   
	 * @return
	 */
	
	long getDefaultRunningComponentLength();
	
	
	
	
	
}
