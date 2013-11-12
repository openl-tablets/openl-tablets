package org.openl.util.ce.impl;

import org.openl.util.ce.conf.IServiceMTConfiguration;


/**
 * Dummy implementation, will use 1.7 later, if necessary. Current measurement did not demonstrate
 * any significant performance advantages of using new ForkJoinPool classes over older implementation for openl tasks.   
 * 
 * @author snshor
 *
 */

public class ServiceMT17 extends  ServiceMT16 {

	public ServiceMT17(IServiceMTConfiguration config) {
		super(config);
	}


}
