package org.openl.rules.vm;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.rules.vm.SimpleRulesRuntimeEnv.Data;

class Storage {
    private final Map<Object, Data> storage = new WeakHashMap<Object, Data>();

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock readLock = readWriteLock.readLock();

    private final Lock writeLock = readWriteLock.writeLock();

    public Data get(Object key) {
        readLock.lock();
        try {
            return storage.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public Data put(Object key, Data data) {
        writeLock.lock();
        try {
            return storage.put(key, data);
        } finally {
            writeLock.unlock();
        }
    }

    public void clear() {
        writeLock.lock();
        try {
            storage.clear();
        } finally {
            writeLock.unlock();
        }
    }

}
