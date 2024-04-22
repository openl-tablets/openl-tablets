package org.openl.rules.cloner;

import java.lang.reflect.Array;

class ArrayImmutableCloner implements ICloner<Object> {

    static final ICloner<Object> theInstance = new ArrayImmutableCloner();

    @Override
    public Object getInstance(Object source) {
        var componentType = source.getClass().getComponentType();
        var length = Array.getLength(source);
        var newArray = Array.newInstance(componentType, length);
        System.arraycopy(source, 0, newArray, 0, length);
        return  newArray;
    }
}
