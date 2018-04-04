package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

public class ArrayIndex implements IOpenIndex {
    private IOpenClass elementType;

    public ArrayIndex(IOpenClass elementType) {
        this.elementType = elementType;
    }

    public IOpenClass getElementType() {
        return elementType;
    }

    public IOpenClass getIndexType() {
        return JavaOpenClass.INT;
    }

    @Override
    public Collection getIndexes(Object container) {
        int length = Array.getLength(container);
        List<Integer> indexes = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            indexes.add(i);
        }
        return indexes;
    }

    public Object getValue(Object container, Object index) {
        Integer ind = (Integer) index;
        if (container == null || ind == null || ind < 0 || ind >= Array.getLength(container)) {
            return getElementType().nullObject();
        }
        return Array.get(container, ind);
    }

    public boolean isWritable() {
        return true;
    }

    public void setValue(Object container, Object index, Object value) {
        Array.set(container, (Integer) index, value);
    }

}
