package org.openl.util.ce;

import org.openl.util.ce.conf.ServiceMTConfiguration;

public interface IServiceMTFactory {

	IServiceMT makeService(ServiceMTConfiguration config);
	
}
