package org.openl.util.ce.impl;

import org.openl.util.ce.IActivity;
import org.openl.util.ce.IServiceMT;
import org.openl.util.ce.conf.IServiceMTConfiguration;
import org.openl.util.ce.conf.ServiceMTConfiguration;

public abstract class ServiceMT extends ServiceBase {

	static private IServiceMT service = new ServiceMTFactory()
	.makeService(new ServiceMTConfiguration());

	static public IServiceMT getService() {
		return service;
	}

	static public void setService(IServiceMT s) {

		service = s;
	}



	public ServiceMT(IServiceMTConfiguration config) {

		super(config);
	}


	protected int calcSplitSize(int length, long durationEstimate) {
		
		long minSize = config.getMinSequenceLengthNs();
		
		int maxSplits = config.getParallelLevel() * 2 + 1;
		
		long totalSizeEstimate = length * durationEstimate;
				
		double busyRatio = getBusyRatio();

		
		maxSplits =  (int) (maxSplits * (1 - busyRatio));
		
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
