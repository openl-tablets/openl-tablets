package org.openl.rules.cloner;

import java.lang.reflect.Array;

/**
 * A special fast implementation to clone array which consists with immutable elements. Immutable elements does not require
 * deep cloning, so this cloner just copies an array as is, using {@linkplain System#arraycopy(Object, int, Object, int, int)}
 * method.
 *
 * @author Yury Molchan
 */
class ArrayImmutableCloner implements ICloner<Object> {

    static final ICloner<Object> theInstance = new ArrayImmutableCloner();

    @Override
    public Object getInstance(Object source) {
        var componentType = source.getClass().getComponentType();
        var length = Array.getLength(source);
        var newArray = Array.newInstance(componentType, length);
        System.arraycopy(source, 0, newArray, 0, length);
        return newArray;
    }
}
