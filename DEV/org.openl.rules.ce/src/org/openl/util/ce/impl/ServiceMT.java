package org.openl.util.ce.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.openl.util.ce.IActivity;
import org.openl.util.ce.IServiceMT;
import org.openl.util.ce.conf.ServiceMTConfiguration;

public abstract class ServiceMT extends ServiceBase {

	static private IServiceMT service = new ServiceMTFactory()
	.makeService(ServiceMTConfiguration.loadProjectResolverFromClassPath());

	static public IServiceMT getService() {
		return service;
	}

	static public void setService(IServiceMT s) {

		service = s;
	}



	@Override
	public int getActiveThreadCounter() {
		return activeThreadCounter.get();
	}

	public ServiceMT(ServiceMTConfiguration config) {

		super(config);
	}

	static AtomicInteger activeThreadCounter = new AtomicInteger(0);

	protected int calcSplitSize(int length, long durationEstimate) {
		
		long minSize = config.getMinSequenceLengthNs();
		
		int maxSplits = config.getMaxPerRequestParallelLevel();
		int activeCounter = getActiveThreadCounter();
		int availableThreads = config.getTotalParallelLevel() - activeCounter;
		
		maxSplits = Math.min(availableThreads, maxSplits);
		
		
		long totalSizeEstimate = length * durationEstimate;
				

		
		
		int maxSplits2 = (int)(totalSizeEstimate / minSize);
		
		int splits = Math.max(1, Math.min(maxSplits, maxSplits2)); 
		
		int splitSize = Math.max(length / splits, 1);
		
		return splitSize;
	}


 	protected long calcTotalEstimate(IActivity[] all) {
 		
 		long sum = 0;
 		for (int i = 0; i < all.length; i++) {
			sum += all[i].duration();
		}
		return sum;
	}

	
	
	
}
