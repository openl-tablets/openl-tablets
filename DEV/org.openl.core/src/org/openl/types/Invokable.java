package org.openl.types;

import org.openl.vm.IRuntimeEnv;

/**
 * Interface for invokable objects.
 *
 * @author DLiauchuk, Yury Molchan
 */
public interface Invokable<T, E extends IRuntimeEnv> {

    /**
     * Java's like reflection functionality to execute methods.
     *
     * @param target the target object agains which is invoked this method. Can be null for a 'static' method.
     * @param params the argument for this method. Can be null.
     * @param env TODO: what is IRuntimeEnv ?
     * @return returns result of this method execution.
     */
    <R> R invoke(T target, Object[] params, E env);
}
