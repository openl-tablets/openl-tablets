package org.openl.types.impl;

import java.lang.reflect.Array;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayElements;

@RequiredArgsConstructor
public class ArrayIndex implements IOpenIndex {
    @Getter
    private final IOpenClass elementType;

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
        return ArrayElements.get(container, ind);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void setValue(Object container, Object index, Object value) {
        ArrayElements.set(container, (Integer) index, value);
    }

}
