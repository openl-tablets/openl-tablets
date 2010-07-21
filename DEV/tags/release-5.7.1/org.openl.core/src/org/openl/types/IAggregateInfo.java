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
    IOpenClass getComponentType(IOpenClass aggregateType);

    /**
     * @return index interface if aggregate is indexable
     */
    IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType);

    IOpenClass getIndexedAggregateType(IOpenClass componentType, int dims);

    Iterator<Object> getIterator(Object aggregate);

    boolean isAggregate(IOpenClass type);

    Object makeIndexedAggregate(IOpenClass componentType, int[] dimValues);

}
