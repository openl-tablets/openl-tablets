/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types;

import java.util.Iterator;

/**
 * @author snshor
 *
 */
public interface IAggregateInfo
{
	public IOpenClass getComponentType(IOpenClass aggregateType);
	public Iterator<Object> getIterator(Object aggregate);
	
	public Object makeIndexedAggregate(IOpenClass componentType, int[] dimValues);
	public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dims);

	
	/**
	 * @return index interface if aggregate is indexable 
	 */
	public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType);

	public boolean isAggregate(IOpenClass type);


}



