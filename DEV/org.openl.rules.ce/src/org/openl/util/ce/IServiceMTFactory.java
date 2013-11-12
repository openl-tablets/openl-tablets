package org.openl.util.ce;

import org.openl.util.ce.conf.IServiceMTConfiguration;

public interface IServiceMTFactory {

	IServiceMT makeService(IServiceMTConfiguration config);
	
}
