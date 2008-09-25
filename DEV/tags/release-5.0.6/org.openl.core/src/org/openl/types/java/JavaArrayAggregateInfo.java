/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.java;

import java.util.Iterator;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class JavaArrayAggregateInfo extends AAggregateInfo
{

	static public final IAggregateInfo ARRAY_AGGREGATE =
		new JavaArrayAggregateInfo();


  /* (non-Javadoc)
   * @see org.openl.types.IAggregateInfo#getComponentType(org.openl.types.IOpenClass)
   */
  public IOpenClass getComponentType(IOpenClass aggregateType)
  {
  	return JavaOpenClass.getOpenClass(aggregateType.getInstanceClass().getComponentType());
  }

  public Iterator<Object> getIterator(Object aggregate)
  {
    return OpenIterator.fromArray(aggregate);
  }

  public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType)
  {
  	if (indexType != JavaOpenClass.INT)
  	  return null;
	  	
    if (!isAggregate(aggregateType))
	return null;
	
    return JavaOpenClass.makeArrayIndex(aggregateType);
  }

  public boolean isAggregate(IOpenClass type)
  {
    return type.getInstanceClass().isArray();
  }

}
