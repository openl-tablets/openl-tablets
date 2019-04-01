/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;

public class JavaMapAggregateInfo implements IAggregateInfo {

    static class MapIndex implements IOpenIndex {
        @Override
        public IOpenClass getElementType() {
            return JavaOpenClass.OBJECT;
        }

        @Override
        public IOpenClass getIndexType() {
            return JavaOpenClass.OBJECT;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object getValue(Object container, Object index) {
            return container == null ? null : ((Map<Object, Object>) container).get(index);
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setValue(Object container, Object index, Object value) {
            ((Map<Object, Object>) container).put(index, value);
        }
    }

    static final IAggregateInfo MAP_AGGREGATE = new JavaMapAggregateInfo();

    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.getOpenClass(Map.Entry.class);
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType) {
        return getIndex(aggregateType, JavaOpenClass.OBJECT);
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (!isAggregate(aggregateType)) {
            return null;
        }

        return new MapIndex();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Map) aggregate).entrySet().iterator();
    }

    @Override
    public boolean isAggregate(IOpenClass type) {
        return Map.class.isAssignableFrom(type.getInstanceClass());
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType) {
        return AAggregateInfo.getArrayType(componentType);
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentType, int size) {
        return new HashMap(size);
    }
}
