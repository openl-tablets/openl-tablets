package org.openl.util.ce;

import org.openl.util.IConvertor;

public interface IMTConvertorFactory<T> {
	
	
	IConvertor<Integer, T> makeConvertorInstance();
	long estimateDuration(Integer index);
	
	
}
