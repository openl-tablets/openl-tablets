/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.*;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;

public class JavaCollectionAggregateInfo implements IAggregateInfo {
    static final IAggregateInfo COLLECTION_AGGREGATE = new JavaCollectionAggregateInfo();

    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.OBJECT;
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType) {
        return getIndex(aggregateType, JavaOpenClass.OBJECT);
    }

    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        // For general collection indexed operator x[i] isn't supported
        // Returned index is limited: it supports only method collection.add(element)
        return new CollectionIndex();
    }


    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dim) {
        return AAggregateInfo.getArrayType(componentType, dim);
    }

    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Collection<Object>) aggregate).iterator();
    }

    public boolean isAggregate(IOpenClass type) {
        return Collection.class.isAssignableFrom(type.getInstanceClass());
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentType, int size) {
        // HashSet is one of Collection implementations, so it's legal here.
        // If more specific collection type is needed (for example Deque) implement more specific IAggregateInfo for it.
        return new HashSet<>(size);
    }

    private static class CollectionIndex implements IOpenIndex {
        public IOpenClass getElementType() {
            return JavaOpenClass.OBJECT;
        }

        public IOpenClass getIndexType() {
            return null;
        }

        public Object getValue(Object container, Object index) {
            throw new UnsupportedOperationException();
        }

        public boolean isWritable() {
            return true;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object container, Object index, Object value) {
            ((Collection<Object>) container).add(value);
        }
    }
}
