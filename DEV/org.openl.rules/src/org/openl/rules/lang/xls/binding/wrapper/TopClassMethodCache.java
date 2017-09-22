package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

//This is workaround of too much creating MethodKeys in runtime
class TopClassMethodCache {

    private static final class TopClassMethodCacheHolder {
        private final static TopClassMethodCache INSTANCE = new TopClassMethodCache();
    }

    public static TopClassMethodCache getInstance() {
        return TopClassMethodCacheHolder.INSTANCE;
    }

    private Map<IOpenClass, Map<IOpenMethodWrapper, IOpenMethod>> classToWrapperMethodCache = new WeakHashMap<>();
    private ReentrantReadWriteLock wrapperMethodCacheReadWriteLock = new ReentrantReadWriteLock();

    public IOpenMethod getTopClassMethod(IOpenClass topClass, IOpenMethodWrapper wrapper) {
        if (topClass == null) {
            return null;
        }
        Lock lock = wrapperMethodCacheReadWriteLock.readLock();
        try {
            lock.lock();
            Map<IOpenMethodWrapper, IOpenMethod> wrapperToMethodCache = classToWrapperMethodCache.get(topClass);
            if (wrapperToMethodCache != null) {
                IOpenMethod method = wrapperToMethodCache.get(wrapper);
                if (method != null) {
                    return method;
                }
            }
        } finally {
            lock.unlock();
        }
        Lock writeLock = wrapperMethodCacheReadWriteLock.writeLock();
        try {
            writeLock.lock();
            IOpenMethod method = topClass.getMethod(wrapper.getDelegate().getName(),
                wrapper.getDelegate().getSignature().getParameterTypes());
            Map<IOpenMethodWrapper, IOpenMethod> wrapperToMethodCache = classToWrapperMethodCache.get(topClass);
            if (wrapperToMethodCache == null) {
                wrapperToMethodCache = new WeakHashMap<>();
                classToWrapperMethodCache.put(topClass, wrapperToMethodCache);
            }
            wrapperToMethodCache.put(wrapper, method);
            return method;
        } finally {
            writeLock.unlock();
        }
    }
}
