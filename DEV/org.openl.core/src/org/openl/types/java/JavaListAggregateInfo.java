/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;

/**
 * @author snshor
 *
 */
public class JavaListAggregateInfo extends AAggregateInfo {

    static class ListIndex implements IOpenIndex {

        @Override
        public IOpenClass getElementType() {
            return JavaOpenClass.OBJECT;
        }

        @Override
        public IOpenClass getIndexType() {
            return JavaOpenClass.INT;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object getValue(Object container, Object index) {
            if (container == null || index == null) {
                return null;
            }
            return ((List<Object>) container).get((Integer) index);
        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setValue(Object container, Object index, Object value) {
            ((List<Object>) container).set((Integer) index, value);
        }
    }

    public static final IAggregateInfo LIST_AGGREGATE = new JavaListAggregateInfo();

    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.OBJECT;
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (!JavaOpenClass.INT.equals(indexType) && !Integer.class.equals(indexType.getInstanceClass())) {
            return null;
        }

        if (!isAggregate(aggregateType)) {
            return null;
        }

        return new ListIndex();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Collection<Object>) aggregate).iterator();
    }

    @Override
    public boolean isAggregate(IOpenClass type) {
        return List.class.isAssignableFrom(type.getInstanceClass());
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentClass, int size) {
        ArrayList<Object> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(null);
        }
        return list;
    }
}
