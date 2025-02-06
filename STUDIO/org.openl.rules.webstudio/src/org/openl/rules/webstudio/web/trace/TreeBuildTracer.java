/**
 * Created Dec 3, 2006
 */
package org.openl.rules.webstudio.web.trace;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.domain.IIntSelector;
import org.openl.exception.OpenLUserRuntimeException;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.webstudio.web.trace.node.CachingArgumentsCloner;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.LazyTracerNodeObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;
import org.openl.rules.webstudio.web.trace.node.SimpleTracerObject;
import org.openl.rules.webstudio.web.trace.node.SpreadsheetTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.TracedObjectFactory;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * @author Yury Molchan
 */
public final class TreeBuildTracer extends Tracer {

    private final static Logger LOG = LoggerFactory.getLogger(TreeBuildTracer.class);
    private static final ThreadLocal<ITracerObject> tree = new ThreadLocal<>();
    private static final ThreadLocal<Map<TracerKeyNode, SimpleTracerObject>> map = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> lazyNodesHolder = ThreadLocal.withInitial(() -> false);

    static {
        Tracer.instance = new TreeBuildTracer();
    }

    @Override
    protected void doPut(Object source, String id, Object... args) {
        if (!isOn() || isLazy()) {
            return;
        }
        ITracerObject trObj = TracedObjectFactory.getTracedObject(source, id, args);

        if (trObj != null) {
            doPut(trObj);
        }
    }

    private void doPut(ITracerObject obj) {
        ITracerObject current = tree.get();
        current.addChild(obj);
    }

    private void doBegin(ITracerObject obj) {
        if (!isOn()) {
            return;
        }
        doPut(obj);
        tree.set(obj);
    }

    private void doEnd() {
        if (!isOn()) {
            return;
        }
        ITracerObject current = tree.get();
        if (current != null) {
            tree.set(current.getParent());
        } else {
            LOG.warn("Something is wrong. Current trace object is null. Cannot pop trace object.");
        }
    }

    @Override
    protected <T, E extends IRuntimeEnv, R> R doInvoke(Invokable<? super T, E> executor,
                                                       T target,
                                                       Object[] params,
                                                       E env,
                                                       Object source) {
        if (!isOn() || isLazy()) {
            // Skip if tracing is switched off
            return executor.invoke(target, params, env);
        }

        ITracerObject trObj;
        if (canBeLazyNode(source)) {
            trObj = new LazyTracerNodeObject(executor, target, params, env, source, Thread.currentThread().getContextClassLoader());
        } else {
            trObj = TracedObjectFactory.getTracedObject(source, executor, target, params, env);
        }
        if (trObj == null) {
            // Skip if no tracing objects are
            return executor.invoke(target, params, env);
        }
        doBegin(trObj);
        try {
            R res = executor.invoke(target, params, env);
            if (trObj instanceof SimpleTracerObject) {
                ((SimpleTracerObject) trObj).setResult(res);
            }
            if (trObj instanceof SpreadsheetTracerLeaf) {
                cacheNode(new TracerKeyNode<>(executor, target, params, env, source), (SpreadsheetTracerLeaf) trObj);
            }
            return res;
        } catch (Throwable ex) {
            if (trObj instanceof SimpleTracerObject) {
                ((SimpleTracerObject) trObj).setError(ex);
            }
            throw ex;
        } finally {
            doEnd();
        }
    }

    private static boolean canBeLazyNode(Object source) {
        return Boolean.TRUE.equals(lazyNodesHolder.get())
                && !(tree.get() instanceof TracerRootNodeObject)
                && TracedObjectFactory.supportLazyTrace(source);
    }

    @Override
    protected <T> T doWrap(Object source, T target, Object[] args) {
        if (!isOn()) {
            return target;
        } else if (target instanceof IIntSelector) {
            return (T) new IntSelectorTracer((IIntSelector) target, (ICondition) args[0]);
        }
        return target;
    }

    @Override
    public boolean isOn() {
        return tree.get() != null;
    }

    private boolean isLazy() {
        return tree.get() instanceof LazyTracerNodeObject;
    }

    public static ITracerObject performLazyTracerNode(LazyTracerNodeObject lazyNode) {
        ClassLoader currentCtxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(lazyNode.getClassLoader());
            CachingArgumentsCloner.initInstance(lazyNode.getCacheCloner());
            var rootNode = initialize(true);
            try {
                invoke(lazyNode.getExecutor(), lazyNode.getTarget(), lazyNode.getParams(), lazyNode.getEnv(), lazyNode.getSource());
            } catch (OpenLUserRuntimeException ignored) {
            } catch (Throwable ex) {
                LOG.warn("Error during lazy node execution", ex);
            }
            ITracerObject realNode = null;
            for (var childNode : rootNode.getChildren()) {
                if (realNode != null) {
                    throw new IllegalStateException("Something is wrong. More than one child node is created");
                }
                realNode = childNode;
            }
            var parentNode = lazyNode.getParent();
            realNode.setParent(parentNode);
            parentNode.replace(lazyNode, realNode);
            return realNode;
        } finally {
            CachingArgumentsCloner.removeInstance();
            Thread.currentThread().setContextClassLoader(currentCtxClassLoader);
            destroy();
        }
    }

    public static ITracerObject initialize(boolean lazyNodes) {
        lazyNodesHolder.set(lazyNodes);
        var root = new TracerRootNodeObject();
        tree.set(root);
        return root;
    }

    public static void destroy() {
        tree.set(null);
        map.set(null);
    }

    private void cacheNode(TracerKeyNode key, SimpleTracerObject value) {
        Map<TracerKeyNode, SimpleTracerObject> localCache = map.get();
        if (localCache == null) {
            localCache = new HashMap<>();
            map.set(localCache);
        }
        ITracerObject prevValue = localCache.put(key, value);
        if (prevValue != null) {
            LOG.warn("Something is wrong. Previous tracer node is not null");
        }
    }

    @Override
    protected <T, E extends IRuntimeEnv> boolean doResolveTraceNode(Invokable<? super T, E> executor,
                                                                 T target,
                                                                 Object[] params,
                                                                 E env,
                                                                 Object source) {
        if (!isOn()) {
            return false;
        }

        Map<TracerKeyNode, SimpleTracerObject> localCache = map.get();
        if (localCache != null) {
            SimpleTracerObject node = localCache.get(new TracerKeyNode<>(executor, target, params, env, source));
            if (node != null) {
                ITracerObject newNode = new RefToTracerNodeObject(node);
                doPut(newNode);
                return true;
            }
        }
        if (!isLazy() && canBeLazyNode(source)) {
            var lazyNode = new LazyTracerNodeObject(executor, target, params, env, source, Thread.currentThread().getContextClassLoader());
            doPut(lazyNode);
            return true;
        }
        return false;
    }

    private static class TracerRootNodeObject extends SimpleTracerObject {

        public TracerRootNodeObject() {
            super("traceroot");
        }

        @Override
        public String getUri() {
            return null;
        }

        @Override
        public Object getResult() {
            return null;
        }
    }
}
