package org.openl.rules.cloner;

import java.lang.reflect.Array;
import java.util.function.Function;

/**
 * This cloner do cloning of the array.
 *
 * @author Yury Molchan
 */
class ArrayCloner implements ICloner<Object> {

    final static ICloner<Object> theInstance = new ArrayCloner();

    @Override
    public Object getInstance(Object source) {
        var componentType = source.getClass().getComponentType();
        var length = Array.getLength(source);
        return Array.newInstance(componentType, length);
    }

    public void clone(Object o, Function<Object, Object> cloner, Object target) {
        var length = Array.getLength(target);
        for (int i = 0; i < length; i++) {
            var element = Array.get(o, i);
            Array.set(target, i, cloner.apply(element));
        }
    }
}
