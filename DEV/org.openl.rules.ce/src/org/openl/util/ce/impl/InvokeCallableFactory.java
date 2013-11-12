package org.openl.util.ce.impl;

import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.ce.IActivity;
import org.openl.util.ce.ICallableActivity;
import org.openl.util.ce.InvokeFactory;

public class InvokeCallableFactory implements InvokeFactory {

	@Override
	public Object invoke(IActivity activity) {
		
		try {
			return invokeCallable((ICallableActivity<?>)activity);
		} catch (Exception e) {
			throw RuntimeExceptionWrapper.wrap(e);
		}
		
	}
	
	
	
	public  Object invokeCallable(ICallableActivity<?> act) throws Exception
	{
		return act.call();
	}
	
	
	

}
