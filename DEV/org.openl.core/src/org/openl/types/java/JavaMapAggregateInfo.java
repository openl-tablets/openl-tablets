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

public class JavaMapAggregateInfo implements IAggregateInfo {

    static class MapIndex implements IOpenIndex {
        public IOpenClass getElementType() {
            return JavaOpenClass.OBJECT;
        }

        public IOpenClass getIndexType() {
            return JavaOpenClass.OBJECT;
        }

        @SuppressWarnings("unchecked")
        public Object getValue(Object container, Object index) {
            return container == null ? null : ((Map<Object, Object>) container).get(index);
        }

        public boolean isWritable() {
            return true;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object container, Object index, Object value) {
            ((Map<Object, Object>) container).put(index, value);
        }
    }

    static final IAggregateInfo MAP_AGGREGATE = new JavaMapAggregateInfo();

    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.getOpenClass(Map.Entry.class);
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType) {
        return getIndex(aggregateType, JavaOpenClass.OBJECT);
    }

    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (!isAggregate(aggregateType)) {
            return null;
        }

        return new MapIndex();
    }

    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Map) aggregate).entrySet().iterator();
    }

    public boolean isAggregate(IOpenClass type) {
        return Map.class.isAssignableFrom(type.getInstanceClass());
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dim) {
        return AAggregateInfo.getArrayType(componentType, dim);
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentType, int size) {
        return new HashMap(size);
    }
}
