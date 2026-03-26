package org.openl.rules.webstudio.web.trace.node;


import lombok.extern.slf4j.Slf4j;

import org.openl.rules.cloner.Cloner;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;

@Slf4j
public class LazyTracerNodeObject implements ITracerObject {


    private ITracerObject parent;

    private final IRuntimeEnv env;
    private final Invokable executor;
    private final Object target;
    private final Object[] params;
    private final Object source;
    private final ClassLoader classLoader;
    private final CachingArgumentsCloner<?> cacheCloner;

    public LazyTracerNodeObject(Invokable executor,
                                Object target,
                                Object[] params,
                                IRuntimeEnv env,
                                Object source,
                                ClassLoader classLoader) {
        this.env = env.copy();
        if (env instanceof SimpleRuntimeEnv runtimeEnv) {
            ((SimpleRuntimeEnv) this.env).setMethodWrapper(runtimeEnv.getMethodWrapper());
            ((SimpleRuntimeEnv) this.env).setTopClass(runtimeEnv.getTopClass());
        }
        this.executor = executor;
        this.target = target;
        this.params = clone(params);
        this.source = source;
        this.classLoader = classLoader;
        this.cacheCloner = CachingArgumentsCloner.getInstance();
    }

    public IRuntimeEnv getEnv() {
        return env;
    }

    public Invokable getExecutor() {
        return executor;
    }

    public Object getTarget() {
        return target;
    }

    public Object[] getParams() {
        return params;
    }

    public Object getSource() {
        return source;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public CachingArgumentsCloner<?> getCacheCloner() {
        return cacheCloner;
    }

    @Override
    public ITracerObject getParent() {
        return parent;
    }

    @Override
    public void setParent(ITracerObject parent) {
        this.parent = parent;
    }

    @Override
    public void addChild(ITracerObject child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<ITracerObject> getChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getResult() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLeaf() {
        throw new UnsupportedOperationException();
    }

    private <T> T clone(T obj) {
        try {
            return Cloner.clone(obj);
        } catch (Exception e) {
            log.debug("Ignored error: ", e);
            return obj;
        }
    }

    @Override
    public void replace(LazyTracerNodeObject lazyNode, ITracerObject realNode) {
        throw new UnsupportedOperationException();
    }
}
