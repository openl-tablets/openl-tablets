package org.openl.rules.vm;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.cloner.Cloner;

public class ArgumentCachingStorage {
    private static final Logger LOG = LoggerFactory.getLogger(ArgumentCachingStorage.class);

    private final Storage storage = new Storage();

    public Object findInCache(Object member, Object... params) throws ResultNotFoundException {
        Data data = storage.get(member);
        if (data != null) {
            return data.get(params);
        }
        throw new ResultNotFoundException();
    }

    public void putToCache(Object member, Object[] params, Object result) {
        Data data = storage.get(member);
        if (data == null) {
            data = new Data();
            storage.put(member, data);
        }
        try {
            data.get(params);
        } catch (ResultNotFoundException e) {
            Object[] clonedParams = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    clonedParams[i] = Cloner.clone(params[i]);
                }
            }
            data.add(new InvocationData(clonedParams, result));
            LOG.debug("Error occurred: ", e);
        }
    }

    static final class InvocationData {
        private final Object[] params;
        private int paramsHashCode;
        private boolean paramsHashCodeCalculated;
        private final Object result;

        public InvocationData(Object[] params, Object result) {
            this.params = params;
            this.result = result;
        }

        public Object[] getParams() {
            return params;
        }

        public int getParamsHashCode() {
            if (!paramsHashCodeCalculated) {
                paramsHashCodeCalculated = true;
                paramsHashCode = Arrays.deepHashCode(getParams());
            }
            return paramsHashCode;
        }

        public Object getResult() {
            return result;
        }
    }

    static final class Data {
        private static final int MAX_DATA_LENGTH = 1000;

        final InvocationData[] invocationDatas = new InvocationData[MAX_DATA_LENGTH];
        int size;

        public Object get(Object[] params) throws ResultNotFoundException {
            int hashCode = Arrays.deepHashCode(params);

            for (int i = 0; i < size; i++) {
                InvocationData invocationData = invocationDatas[i];
                if (hashCode == invocationData.getParamsHashCode()
                        && Arrays.deepEquals(invocationData.getParams(), params)) {
                    return invocationData.getResult();
                }
            }
            throw new ResultNotFoundException();
        }

        public void add(InvocationData invocationData) {
            if (size < MAX_DATA_LENGTH) {
                invocationDatas[size] = invocationData;
                size++;
            }
        }
    }

    static class Storage {
        private final Map<Object, Data> storage = new WeakHashMap<>();

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
}
