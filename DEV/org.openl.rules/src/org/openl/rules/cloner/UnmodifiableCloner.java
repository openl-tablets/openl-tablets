package org.openl.rules.cloner;

import java.util.function.Function;

/**
 * A cloner for filling of the unmodifiable wrappers via the modifiable target instance.
 *
 * @author Yury Molchan
 */
final class UnmodifiableCloner<T> implements ICloner<T> {

    private ICloner<? super T> cloner;
    private Function<T, Object> instantiator;

    static <T> ICloner<T> create(Function<T, Object> instantiator, ICloner<? super T> cloner) {
        var r = new UnmodifiableCloner<T>();
        r.cloner = cloner;
        r.instantiator = instantiator;
        return r;
    }

    @Override
    public Object getInstance(T source) {
        var x = new Wrapper();
        x.target = cloner.getInstance(source);
        x.unmodifiable = instantiator.apply((T) x.target);
        return x;
    }

    @Override
    public void clone(T source, Function<Object, Object> cloner, T target) {
        this.cloner.clone(source, cloner, target);
    }
}
