/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types;

import java.util.Map;

/**
 * @author snshor
 *
 */
public interface IDynamicObject
{

	IOpenClass getType();
	
	Object getFieldValue(String name);
	void setFieldValue(String name, Object value);

	Map<String, Object> getFieldValues();

}
