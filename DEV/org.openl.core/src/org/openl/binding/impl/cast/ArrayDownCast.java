package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.openl.types.IOpenClass;

public class ArrayDownCast implements IOpenCast {

    private IOpenClass to;
    private IOpenCast openCast;

    public ArrayDownCast(IOpenClass to, IOpenCast openCast) {
        if (to == null) {
            throw new IllegalArgumentException("to arg can't be null!");
        }
        if (!to.isArray()){
            throw new IllegalArgumentException("to arg should be array type!");
        }
        this.to = to;
        this.openCast = openCast;
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        Class<?> componentClass = to.getComponentClass().getInstanceClass();
        int length = Array.getLength(from);
        Object convertedArray = Array.newInstance(componentClass, length);
        for (int i = 0;i<length;i++){
            Object f = Array.get(from, i);
            Object t = openCast.convert(f);
            Array.set(convertedArray, i, t);
        }
        return convertedArray;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return openCast.getDistance(from, to);
    }

    public boolean isImplicit() {
        return false;
    }

}
