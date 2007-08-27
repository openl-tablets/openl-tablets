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

  /* (non-Javadoc)
   * @see org.openl.types.IAggregateInfo#getIterator(java.lang.Object)
   */
  public Iterator getIterator(Object aggregate)
  {
    return OpenIterator.fromArray(aggregate);
  }

  /* (non-Javadoc)
   * @see org.openl.types.IAggregateInfo#getIndex(org.openl.types.IOpenClass)
   */
  public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType)
  {
  	if (indexType != JavaOpenClass.INT)
  	  return null;
	  	
    return JavaOpenClass.makeArrayIndex(aggregateType);
  }

  /* (non-Javadoc)
   * @see org.openl.types.IAggregateInfo#isAggregate()
   */
  public boolean isAggregate(IOpenClass type)
  {
    return type.getInstanceClass().isArray();
  }

}
