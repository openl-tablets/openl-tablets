package org.openl.types;

import java.util.Iterator;

/**
 * @author snshor
 *
 */
public interface IAggregateInfo {
    IOpenClass getComponentType(IOpenClass aggregateType);

    /**
     * Get index for aggregate type with default index type for this IAggregateInfo implementation.
     *
     * @param aggregateType The type from which the index will be created
     * @return index if aggregate is indexable
     */
    IOpenIndex getIndex(IOpenClass aggregateType);

    /**
     * Same as {{@link #getIndex(IOpenClass)}} but index will be converted from indexType to default index type for this IAggregateInfo implementation.
     *
     * @param aggregateType The type from which the index will be created
     * @param indexType Index type from which the index will be converted to internal index type.
     * @return index if aggregate is indexable
     */
    IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType);

    IOpenClass getIndexedAggregateType(IOpenClass componentType);

    Iterator<Object> getIterator(Object aggregate);

    boolean isAggregate(IOpenClass type);

    Object makeIndexedAggregate(IOpenClass componentType, int size);

}
