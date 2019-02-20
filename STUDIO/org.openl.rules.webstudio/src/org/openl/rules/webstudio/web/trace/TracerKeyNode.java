package org.openl.rules.webstudio.web.trace;

import java.util.Arrays;
import java.util.Objects;

import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

public class TracerKeyNode<T, E extends IRuntimeEnv> {

    private Invokable<? super T, E> executor;
    private T target;
    private Object[] params;
    private E env;
    private Object source;

    public TracerKeyNode(Invokable<? super T, E> executor, T target, Object[] params, E env, Object source) {
        this.executor = executor;
        this.target = target;
        this.params = params;
        this.env = env;
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TracerKeyNode<?, ?> that = (TracerKeyNode<?, ?>) o;
        return Objects.equals(executor, that.executor) && Objects.equals(target, that.target) && Arrays.equals(params,
            that.params) && Objects.equals(env, that.env) && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(executor, target, env, source);
        return 31 * result;
    }
}
