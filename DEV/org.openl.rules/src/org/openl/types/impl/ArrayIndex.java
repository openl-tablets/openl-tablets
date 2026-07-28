package org.openl.types.impl;

import java.lang.reflect.Array;

import lombok.RequiredArgsConstructor;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

@RequiredArgsConstructor
public class ArrayIndex implements IOpenIndex {
    private final IOpenClass elementType;

    @Override
    public IOpenClass getElementType() {
        return elementType;
    }

    @Override
    public IOpenClass getIndexType() {
        return JavaOpenClass.INT;
    }

    @Override
    public Object getValue(Object container, Object index) {
        var ind = (Integer) index;
        if (container == null || ind == null || ind < 0 || ind >= Array.getLength(container)) {
            return getElementType().nullObject();
        }
        return Array.get(container, ind);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void setValue(Object container, Object index, Object value) {
        Array.set(container, (Integer) index, value);
    }

}
