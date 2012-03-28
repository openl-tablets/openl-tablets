package org.openl.util.ce.impl;

import org.openl.util.ce.IServiceMTConfiguration;

public class ServiceMTConfiguration implements IServiceMTConfiguration {

	int parallelLevel = Runtime.getRuntime().availableProcessors();
	int errorLimit = 10;
	long minSequenceLengthNs = 500000;
	
	
	public int getParallelLevel() {
		return parallelLevel;
	}
	public void setParallelLevel(int parallelLevel) {
		this.parallelLevel = parallelLevel;
	}
	public int getErrorLimit() {
		return errorLimit;
	}
	public void setErrorLimit(int errorLimit) {
		this.errorLimit = errorLimit;
	}
	public long getMinSequenceLengthNs() {
		return minSequenceLengthNs;
	}
	public void setMinSequenceLengthNs(long minSequenceLengthNs) {
		this.minSequenceLengthNs = minSequenceLengthNs;
	}
	

}
