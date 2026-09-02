package org.openl.rules.cloner;

import java.util.function.Function;

/**
 * The cloner interface.
 *
 * @author Yury Molchan
 */
interface ICloner<T> {
    ICloner<?> doNotClone = source -> source;

    static <T> ICloner<T> create(Function<T, T> instantiator) {
        return instantiator::apply;
    }

    Object getInstance(T source);

    default void clone(T source, Function<Object, Object> cloner, T target) {
    }
}
