package org.openl.rules.vm;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM.SimpleRuntimeEnv;

public class SimpleRulesRuntimeEnv extends SimpleRuntimeEnv {
    private volatile boolean methodArgumentsCacheEnable = false;
    private volatile CacheMode cacheMode = CacheMode.READ_ONLY;
    private volatile boolean ignoreRecalculate = true;
    private volatile boolean originalCalculation = true;
    private ArgumentCachingStorage argumentCachingStorage;
    private ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private Map<Object, Map<String, Object>> transientFieldValues = new HashMap<>();
    private final ReadWriteLock transientFieldValuesLock = new ReentrantReadWriteLock();

    public SimpleRulesRuntimeEnv() {
        super();
    }

    private SimpleRulesRuntimeEnv(SimpleRulesRuntimeEnv env) {
        super(env);
        this.argumentCachingStorage = env.getArgumentCachingStorage();
        this.methodArgumentsCacheEnable = env.methodArgumentsCacheEnable;
        this.cacheMode = env.cacheMode;
        this.ignoreRecalculate = env.ignoreRecalculate;
        this.originalCalculation = env.originalCalculation;
        this.transientFieldValues = env.transientFieldValues;
        this.queue = env.queue;
    }

    @Override
    public IRuntimeEnv clone() {
        return new SimpleRulesRuntimeEnv(this);
    }

    public ArrayDeque<IRuntimeContext> cloneContextStack() {
        return new ArrayDeque<>(contextStack);
    }

    @Override
    protected IRuntimeContext buildDefaultRuntimeContext() {
        return RulesRuntimeContextFactory.buildRulesRuntimeContext();
    }

    public boolean isMethodArgumentsCacheEnable() {
        return methodArgumentsCacheEnable;
    }

    public void changeMethodArgumentsCacheMode(CacheMode mode) {
        this.cacheMode = mode;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setMethodArgumentsCacheEnable(boolean enable) {
        this.methodArgumentsCacheEnable = enable;
    }

    public boolean isIgnoreRecalculation() {
        return ignoreRecalculate;
    }

    public void setIgnoreRecalculate(boolean ignoreRecalculate) {
        this.ignoreRecalculate = ignoreRecalculate;
    }

    public boolean isOriginalCalculation() {
        return originalCalculation;
    }

    public void setOriginalCalculation(boolean originalCalculation) {
        this.originalCalculation = originalCalculation;
    }

    public Object getTransientFieldValue(Object instance, String fieldName) {
        final Lock readLock = transientFieldValuesLock.readLock();
        try {
            readLock.lock();
            Map<String, Object> values = transientFieldValues.get(new IdentityWeakReference<>(instance, queue));
            return values != null ? values.get(fieldName) : null;
        } finally {
            readLock.unlock();
        }
    }

    public void setTransientFieldValue(Object instance, String fieldName, Object value) {
        if (value != null) {
            final Lock writeLock = transientFieldValuesLock.writeLock();
            try {
                writeLock.lock();
                Object zombie = queue.poll();
                while (zombie != null) {
                    transientFieldValues.remove(zombie);
                    zombie = queue.poll();
                }
                Map<String, Object> values = transientFieldValues
                    .computeIfAbsent(new IdentityWeakReference<>(instance, queue), e -> new HashMap<>());
                values.put(fieldName, value);
            } finally {
                writeLock.unlock();
            }
        }
    }

    public ArgumentCachingStorage getArgumentCachingStorage() {
        if (argumentCachingStorage == null) {
            argumentCachingStorage = new ArgumentCachingStorage(this);
        }
        return argumentCachingStorage;
    }

    public IOpenClass getTopClass() {
        return topClass;
    }

    public void setTopClass(IOpenClass topClass) {
        this.topClass = topClass;
    }

    private IOpenClass topClass;

    private Queue<RecursiveAction> actionStack = null;

    public void pushAction(RecursiveAction action) {
        if (actionStack == null) {
            actionStack = new LinkedList<>();
        }
        actionStack.add(action);
    }

    public boolean joinActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            RecursiveAction action = actionStack.poll();
            action.join();
            return true;
        }
        return false;
    }

    public boolean cancelActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            RecursiveAction action = actionStack.poll();
            action.cancel(true);
            return true;
        }
        return false;
    }
}
