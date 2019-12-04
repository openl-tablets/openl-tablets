/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;
import org.openl.util.ClassUtils;

public class JavaCollectionAggregateInfo implements IAggregateInfo {
    static final IAggregateInfo COLLECTION_AGGREGATE = new JavaCollectionAggregateInfo();

    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.OBJECT;
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType) {
        return null;
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        return null;
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType) {
        return AAggregateInfo.getArrayType(componentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Collection<Object>) aggregate).iterator();
    }

    @Override
    public boolean isAggregate(IOpenClass type) {
        return ClassUtils.isAssignable(type.getInstanceClass(), Collection.class);
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentType, int size) {
        // HashSet is one of Collection implementations, so it's legal here.
        // If more specific collection type is needed (for example Deque) implement more specific IAggregateInfo for it.
        return new HashSet<>(size);
    }

}
