/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.vm;

import org.openl.IOpenRunner;

/**
 * @author snshor
 *
 */
public interface IRuntimeEnv
{
	IOpenRunner getRunner();
	
	void pushThis(Object thisObject);
	Object popThis();
	
	Object getThis();
	
	void pushLocalFrame(Object[] frame);
	
	Object[] popLocalFrame();
	
	Object[] getLocalFrame();
	
	
}
