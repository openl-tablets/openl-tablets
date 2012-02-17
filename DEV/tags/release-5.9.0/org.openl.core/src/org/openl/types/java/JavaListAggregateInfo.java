/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.iterators.EmptyIterator;
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

        IOpenClass elementType;

        public ListIndex(IOpenClass elementType) {
            this.elementType = elementType;
        }

        public IOpenClass getElementType() {
            return elementType;
        }

        public IOpenClass getIndexType() {
            return JavaOpenClass.INT;
        }

        @SuppressWarnings("unchecked")
        public Object getValue(Object container, Object index) {
            return ((List<Object>) container).get((Integer) index);
        }

        public boolean isWritable() {
            return true;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object container, Object index, Object value) {
            ((List<Object>) container).set((Integer) index, value);
        }
    }

    static public final IAggregateInfo LIST_AGGREGATE = new JavaListAggregateInfo();

    public IOpenClass getComponentType(IOpenClass aggregateType) {

        // TODO get component type info using Java reflection API?
        // Class<?> listClass = aggregateType.getInstanceClass();

        // TypeVariable<?> t = listClass.getTypeParameters()[0];

        return JavaOpenClass.OBJECT;
    }

    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (indexType != JavaOpenClass.INT) {
            return null;
        }

        if (!isAggregate(aggregateType)) {
            return null;
        }

        return makeListIndex(aggregateType);
    }

    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        // temporary return an empty iterator
        // While the possibility to get an iterator from list won`t be added
        // TODO: add the possibility to get an iterator from list
        return EmptyIterator.INSTANCE;        
    }

    public boolean isAggregate(IOpenClass type) {
        return true;
    }

    private IOpenIndex makeListIndex(IOpenClass aggregateType) {
        return new ListIndex(getComponentType(aggregateType));
    }

}
