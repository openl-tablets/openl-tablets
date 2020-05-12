package org.openl.rules.vm;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.openl.rules.table.OpenLCloner;

public class TransientFieldsValues {
    public final static class NullValue {
        private NullValue() {
        }
    }

    private static final NullValue NULL_VALUE = new NullValue();

    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final Map<Object, Object> values = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Supplier<Object> defaultValueSupplier = null;

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
        Object v;
        final Lock readLock = lock.readLock();
        try {
            readLock.lock();
            v = this.values.get(new IdentityWeakReference<>(instance, queue));
        } finally {
            readLock.unlock();
        }
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
        final Lock readLock = lock.readLock();
        try {
            readLock.lock();
            return this.values.containsKey(new IdentityWeakReference<>(instance, queue));
        } finally {
            readLock.unlock();
        }
    }

    public void setValue(Object instance, Object value) {
        final Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            Object zombie = queue.poll();
            while (zombie != null) {
                values.remove(zombie);
                zombie = queue.poll();
            }
            this.values.put(new IdentityWeakReference<>(instance, queue), value != null ? value : NULL_VALUE);
        } finally {
            writeLock.unlock();
        }
    }

}
