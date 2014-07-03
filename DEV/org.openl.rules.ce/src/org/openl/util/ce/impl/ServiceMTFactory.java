package org.openl.util.ce.impl;

import org.openl.util.ce.IServiceMT;
import org.openl.util.ce.IServiceMTFactory;
import org.openl.util.ce.conf.ServiceMTConfiguration;

public class ServiceMTFactory implements IServiceMTFactory {

	
	static final String SINCE_1_7 = "java.util.concurrent.ForkJoinTask";
	
	@Override
	public IServiceMT makeService(ServiceMTConfiguration config) {
		if (config.getTotalParallelLevel() <= 1)
			return new ServiceST(config);
		
// TODO uncomment if performance gets improved for future versions		
//		try {
//			if (this.getClass().getClassLoader().loadClass(SINCE_1_7) != null)
//				return new ServiceFactory17().makeService(config);
//		} catch (ClassNotFoundException e) {
//		}	

		return new ServiceFactory16().makeService(config);
		
	}

	protected IServiceMT makeServiceInternal(ServiceMTConfiguration config)
	{
		throw new UnsupportedOperationException("Should be implemented in subclass");
	}
	
	
}
