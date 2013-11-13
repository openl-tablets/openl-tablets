package org.openl.util.ce.impl;

import org.openl.util.ce.IServiceMT;
import org.openl.util.ce.IServiceMTFactory;
import org.openl.util.ce.conf.IServiceMTConfiguration;

public class ServiceMTFactory implements IServiceMTFactory {

	
	static final String SINCE_1_7 = "java.util.concurrent.ForkJoinTask";
	
	@Override
	public IServiceMT makeService(IServiceMTConfiguration config) {
		if (config.getParallelLevel() <= 1)
			return new ServiceST(config);
		
		
		try {
			if (this.getClass().getClassLoader().loadClass(SINCE_1_7) != null)
				return new ServiceFactory17().makeService(config);
		} catch (ClassNotFoundException e) {
		}	

		return new ServiceFactory16().makeService(config);
		
	}

	protected IServiceMT makeServiceInternal(IServiceMTConfiguration config)
	{
		throw new UnsupportedOperationException("Should be implemented in subclass");
	}
	
	
}
