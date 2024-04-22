package org.openl.rules.cloner;

import java.util.Map;
import java.util.function.Function;

/**
 * An universal cloner for Java Maps.
 *
 * @author Yury Molchan
 */
abstract class MapCloner<T extends Map<Object, Object>> implements ICloner<T> {
    static <T extends Map<Object, Object>, R extends T> MapCloner<T> create(Function<T, R> instantiator) {
        return new MapCloner<T>() {
            @Override
            public R getInstance(T source) {
                return instantiator.apply(source);
            }
        };
    }

    @Override
    public final void clone(T source, Function<Object, Object> cloner, T target) {
        for (final Map.Entry<?, ?> e : source.entrySet()) {
            target.put(cloner.apply(e.getKey()), cloner.apply(e.getValue()));
        }
    }
}
