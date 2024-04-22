package org.openl.rules.cloner;

import java.util.Collection;
import java.util.function.Function;

/**
 * An universal cloner for Java Collections.
 *
 * @author Yury Molchan
 */
abstract class CollectionCloner<T extends Collection<Object>> implements ICloner<T> {
    static <T extends Collection<Object>> CollectionCloner<T> create(Function<T, T> instantiator) {
        return new CollectionCloner<T>() {
            @Override
            public T getInstance(T source) {
                return instantiator.apply(source);
            }
        };
    }

    @Override
    public final void clone(T source, Function<Object, Object> cloner, T target) {
        for (final Object e : source) {
            target.add(cloner.apply(e));
        }
    }
}
