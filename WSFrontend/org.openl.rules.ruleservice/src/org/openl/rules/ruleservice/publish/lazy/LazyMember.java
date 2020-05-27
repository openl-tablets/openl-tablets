package org.openl.rules.ruleservice.publish.lazy;

import org.openl.types.IOpenMember;

/**
 * Lazy IOpenMember that contains info about module where it was declared. When we try to do some operations with lazy
 * member it will compile module and wrap the compiled member.
 *
 * @author Marat Kamalov
 */
public abstract class LazyMember<T extends IOpenMember> {

    private volatile T cachedMember;

    protected abstract T initMember();

    public T getMember() {
        if (cachedMember != null) {
            return cachedMember;
        }
        cachedMember = initMember();
        return cachedMember;
    }

    void clearCachedMember() {
        cachedMember = null;
    }
}
