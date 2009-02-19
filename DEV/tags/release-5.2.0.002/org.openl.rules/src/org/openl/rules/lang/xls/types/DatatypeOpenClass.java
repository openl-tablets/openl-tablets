/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls.types;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DatatypeOpenClass extends ADynamicClass
{

	


	public Object newInstance(IRuntimeEnv env)
	{
		DynamicObject res = new DynamicObject(this);
		return res;
	} 

  /**
   * @param schema
   * @param name
   */
  public DatatypeOpenClass(IOpenSchema schema, String name)
  {
    super(schema, name, DynamicObject.class);
  }
  
  
  
	/**
	 *
	 */

	public IAggregateInfo getAggregateInfo()
	{
		return DynamicArrayAggregateInfo.aggregateInfo;
	}

}
