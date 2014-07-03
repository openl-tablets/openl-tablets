package org.openl.util.ce.impl;

import org.openl.util.ce.IServiceMT;
import org.openl.util.ce.IServiceMTFactory;
import org.openl.util.ce.conf.ServiceMTConfiguration;

public class ServiceFactory17 implements IServiceMTFactory {

	@Override
	public IServiceMT makeService(ServiceMTConfiguration config) {
		return new ServiceMT17(config);
	}

	
	
	
}
