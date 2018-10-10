/**
 * Created Dec 3, 2006
 */
package org.openl.rules.webstudio.web.trace;

import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.SimpleTracerObject;
import org.openl.rules.webstudio.web.trace.node.TracedObjectFactory;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yury Molchan
 */
public final class TreeBuildTracer extends Tracer {

    private final Logger log = LoggerFactory.getLogger(TreeBuildTracer.class);
    private static ThreadLocal<ITracerObject> tree = new ThreadLocal<ITracerObject>();
    private static ThreadLocal<Map<TracerKeyNode, SimpleTracerObject>> map = new ThreadLocal<>();

    static {
        Tracer.instance = new TreeBuildTracer();
    }

    @Override
    protected void doPut(Object source, String id, Object... args) {
        if (!isOn()) {
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
            log.warn("Something is wrong. Current trace object is null. Can't pop trace object.");
        }
    }

    @Override
    protected <T, E extends IRuntimeEnv, R> R doInvoke(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {
        if (!isOn()) {
            // Skip if tracing is switched off
            return executor.invoke(target, params, env);
        }

        SimpleTracerObject trObj = TracedObjectFactory.getTracedObject(source, executor, target, params, env);
        if (trObj == null) {
            // Skip if no tracing objects are
            return executor.invoke(target, params, env);
        }
        doBegin(trObj);
        try {
            R res = executor.invoke(target, params, env);
            trObj.setResult(res);
            cacheNode(new TracerKeyNode<>(executor, target, params, env, source), trObj);
            return res;
        } catch (RuntimeException ex) {
            trObj.setError(ex);
            throw ex;
        } finally {
            doEnd();
        }
    }

    @Override
    protected <T> T doWrap(Object source, T target, Object[] args) {
        if (!isOn()) {
            return target;
        } else if (target instanceof RangeIndex) {
            new RangeIndexTracer((RangeIndex) target, (ICondition) args[0]);
            // No return
        } else if (target instanceof IIntSelector) {
            return (T) new IntSelectorTracer((IIntSelector) target, (ICondition) args[0]);
        }
        return target;
    }

    @Override
    public boolean isOn() {
        return tree.get() != null;
    }

    private static ITracerObject createRoot() {
        return new SimpleTracerObject("traceroot") {

            @Override
            public String getUri() {
                return null;
            }

            public Object getResult() {
                return null;
            }
        };
    }

    public static ITracerObject initialize() {
        ITracerObject root = createRoot();
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
            log.warn("Something is wrong. Previous tracer node is not null");
        }
    }

    @Override
    protected <T, E extends IRuntimeEnv> void doResolveTraceNode(Invokable<? super T, E> executor, T target, Object[] params, E env, Object source) {
        if (!isOn()) {
            return;
        }

        Map<TracerKeyNode, SimpleTracerObject> localCache = map.get();
        if (localCache == null) {
            return;
        }

        SimpleTracerObject node = localCache.get(new TracerKeyNode<>(executor, target, params, env, source));
        if (node != null) {
            SimpleTracerObject newNode = TracedObjectFactory.deepCopy(node);
            doPut(newNode);
        }
    }
}
