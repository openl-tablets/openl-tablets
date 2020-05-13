package org.openl.rules.vm;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.openl.rules.table.OpenLCloner;

public class TransientFieldsValues {
    public final static class NullValue {
        private NullValue() {
        }
    }

    private static final NullValue NULL_VALUE = new NullValue();

    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final Map<Object, Object> values = new ConcurrentHashMap<>();
    private Supplier<Object> defaultValueSupplier = null;
    private final Lock lock = new ReentrantLock();

    public TransientFieldsValues() {
        OpenLCloner.registerTransientFieldsValues(this);
    }

    public TransientFieldsValues(Supplier<Object> defaultValueSupplier) {
        this();
        this.defaultValueSupplier = defaultValueSupplier;
    }

    public void setDefaultValueSupplier(Supplier<Object> defaultValueSupplier) {
        this.defaultValueSupplier = defaultValueSupplier;
    }

    public Object getValue(Object instance) {
        Object v = this.values.get(new IdentityWeakReference<>(instance, queue));
        if (v != null) {
            return v == NULL_VALUE ? null : v;
        }
        if (defaultValueSupplier != null) {
            Object defaultValue = defaultValueSupplier.get();
            setValue(instance, defaultValue);
            return defaultValue;
        }
        return null;
    }

    public boolean hasValue(Object instance) {
        return this.values.containsKey(new IdentityWeakReference<>(instance, queue));
    }

    public void setValue(Object instance, Object value) {
        removeZombies();
        this.values.put(new IdentityWeakReference<>(instance, queue), value != null ? value : NULL_VALUE);
    }

    private void removeZombies() {
        if (lock.tryLock()) {
            try {
                Object zombie = queue.poll();
                while (zombie != null) {
                    values.remove(zombie);
                    zombie = queue.poll();
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
