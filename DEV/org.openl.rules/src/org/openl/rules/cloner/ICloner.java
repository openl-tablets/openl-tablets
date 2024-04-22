package org.openl.rules.cloner;

import java.util.function.Function;

interface ICloner<T> {
    T getInstance(T source);

    default void clone(T source, Function<Object, Object> cloner, T target) {
    }

    static <T> ICloner<T> create(Function<T, T> instantiator) {
        return instantiator::apply;
    }

    static final ICloner<?> doNotClone = source -> source;
}
