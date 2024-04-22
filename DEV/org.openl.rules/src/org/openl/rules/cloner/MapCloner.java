package org.openl.rules.cloner;

import java.util.Map;
import java.util.function.Function;

abstract class MapCloner<T extends Map<Object, Object>> implements ICloner<T> {
    @Override
    public final void clone(T source, Function<Object, Object> cloner, T target) {
        for (final Map.Entry<?, ?> e : source.entrySet()) {
            target.put(cloner.apply(e.getKey()), cloner.apply(e.getValue()));
        }
    }

    static <T extends Map<Object, Object>> MapCloner<T> create(Function<T, T> instantiator) {
        return new MapCloner<T>() {
            @Override
            public T getInstance(T source) {
                return instantiator.apply(source);
            }
        };
    }
}
