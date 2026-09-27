package org.openl.rules.cloner;

import java.util.function.UnaryOperator;

/**
 * The cloner interface.
 *
 * @author Yury Molchan
 */
interface ICloner<T> {
    ICloner<?> doNotClone = source -> source;

    static <T> ICloner<T> create(UnaryOperator<T> instantiator) {
        return instantiator::apply;
    }

    Object getInstance(T source);

    default void clone(T source, UnaryOperator<Object> cloner, T target) {
    }
}
