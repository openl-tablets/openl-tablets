package org.openl.rules.webstudio.web.trace.node;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.cloner.Cloner;

/**
 * This cloner is based on assumption that hashCode() and equals() methods are cheaper than cloning huge objects.
 * <p>
 * If the object overrides hasCode() and equals() methods, it's clone will be reused if it's not changed. If the object
 * does not override hasCode() and equals() methods, then cloned object and original one always will be not equal. So
 * original object can be safely modified, it does not break logic. If equal object is not found in the cache then it
 * will be cloned and stored in the cache.
 * <p>
 * If after clone() new instance is not created (for example, object is immutable), such object is not cached.
 * <p>
 * Clones cannot be changed! If cloned object can be changed in future, this cloner cannot be used. For example, in the
 * trace all arguments are cloned and that cloned objects are never changed, they are used only to store arguments state
 * and show them to the user later. In this case we can safely reuse already cloned object in other method invocation if
 * it's not changed since that.
 */
public final class CachingArgumentsCloner<T> {
    private static final ThreadLocal<CachingArgumentsCloner<?>> instance = new ThreadLocal<>();

    private final Map<Object, Object> cache = new HashMap<>();

    public T clone(T o) {
        return Cloner.clone(o, cache);
    }

    public static <T> CachingArgumentsCloner<T> getInstance() {
        return (CachingArgumentsCloner<T>) instance.get();
    }

    public static void initInstance() {
        instance.set(new CachingArgumentsCloner<>());
    }

    public static void removeInstance() {
        instance.remove();
    }
}
