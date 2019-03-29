package org.openl.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * A cache pool for strings. This util is a replacement for Java's
 * String.intern() but it does not use Perm Gen. All strings in this pool are
 * weak referenced, so they can be garbage collected.
 *
 * Note that this implementation is synchronized.
 *
 * @author Yury Mmolchan
 *
 */
public final class StringPool {
    static final WeakHashMap<String, WeakReference<String>> STRING_POOL = new WeakHashMap<>(5000);

    /**
     * No instantiation.
     */
    private StringPool() {
    }

    /**
     * Returns a canonical representation for the string object. It works like
     * {@link String#intern()} but it uses own pool for collecting unique
     * strings.
     *
     * @see String#intern()
     */
    public static String intern(String value) {
        if (value == null) {
            return null;
        }
        WeakReference<String> ref = STRING_POOL.get(value);
        // Return from the pool if the value exists.
        if (ref != null) {
            String cached = ref.get();
            if (cached != null)
                return cached;
        }

        synchronized (STRING_POOL) {
            ref = STRING_POOL.put(value, new WeakReference<String>(value));
            // Return the placed value if it is absent in the pool.
            if (ref == null) {
                return value;
            }
            String cached = ref.get();
            if (cached == null) {
                return value;
            }

            // Another thread has placed the value the first, so we have to
            // restore this value in the pool.
            STRING_POOL.put(cached, ref);
            return cached;
        }
    }
}
