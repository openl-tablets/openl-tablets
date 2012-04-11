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
public interface IAggregateInfo {
    public IOpenClass getComponentType(IOpenClass aggregateType);

    /**
     * @return index interface if aggregate is indexable
     */
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType);

    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dims);

    public Iterator<Object> getIterator(Object aggregate);

    public boolean isAggregate(IOpenClass type);

    public Object makeIndexedAggregate(IOpenClass componentType, int[] dimValues);

}
