package org.openl.rules.vm;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class IdentityWeakReference<K> extends WeakReference<K> {
    final int hash;

    IdentityWeakReference(K obj, ReferenceQueue<K> queue) {
        super(obj, queue);
        hash = System.identityHashCode(obj);
    }

    public int hashCode() {
        return hash;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IdentityWeakReference) {
            IdentityWeakReference<?> ref = (IdentityWeakReference<?>) o;
            return this.get() == ref.get();
        }
        return false;
    }
}
