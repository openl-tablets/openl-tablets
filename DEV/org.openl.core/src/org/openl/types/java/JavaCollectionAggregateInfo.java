/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Collection;
import java.util.Iterator;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

public class JavaCollectionAggregateInfo implements IAggregateInfo {
    static final IAggregateInfo COLLECTION_AGGREGATE = new JavaCollectionAggregateInfo();

    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.OBJECT;
    }

    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        // For general collection indexed operator x[i] isn't supported
        return null;
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dims) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Collection<Object>) aggregate).iterator();
    }

    public boolean isAggregate(IOpenClass type) {
        return Collection.class.isAssignableFrom(type.getInstanceClass());
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentType, int[] dimValues) {
        throw new UnsupportedOperationException();
    }
}
