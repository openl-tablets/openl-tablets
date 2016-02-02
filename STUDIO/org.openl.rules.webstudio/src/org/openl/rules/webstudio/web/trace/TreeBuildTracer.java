/**
 * Created Dec 3, 2006
 */
package org.openl.rules.webstudio.web.trace;

import org.openl.domain.IIntSelector;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.ScoreAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.RangeIndex;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.MatchTraceObject;
import org.openl.rules.webstudio.web.trace.node.ResultTraceObject;
import org.openl.rules.webstudio.web.trace.node.SimpleTracerObject;
import org.openl.rules.webstudio.web.trace.node.SpreadsheetTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.TracedObjectFactory;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yury Molchan
 */
public final class TreeBuildTracer extends Tracer {

    private final Logger log = LoggerFactory.getLogger(TreeBuildTracer.class);
    private static ThreadLocal<ITracerObject> tree = new ThreadLocal<ITracerObject>();

    static {
        Tracer.instance = new TreeBuildTracer();
    }

    @Override
    protected void doPut(Object source, String id, Object... args) {
        if (!isOn()) {
            return;
        }
        ITracerObject trObj = getTracedObject(source, id, args);

        if (trObj != null) {
            doPut(trObj);
        }
    }

    private static ITracerObject getTracedObject(Object source, String id, Object[] args) {
        ITracerObject trObj;
        if (source instanceof DecisionTableOptimizedAlgorithm) {
            trObj = DTRuleTraceObject.create(args);
        } else if (source instanceof RangeIndexTracer) {
            trObj = DTRuleTraceObject.create(args);
        } else if (source instanceof IntSelectorTracer) {
            trObj = DTRuleTraceObject.create(args);
        } else if (source instanceof MatchAlgorithmExecutor) {
            if ("match".equals(id)) {
                trObj = MatchTraceObject.create(args);
            } else {
                trObj = ResultTraceObject.create(args);
            }
        } else if (source instanceof WeightAlgorithmExecutor) {
            if ("match".equals(id)) {
                trObj = MatchTraceObject.create(args);
            } else {
                trObj = ResultTraceObject.create(args);
            }
        } else if (source instanceof ScoreAlgorithmExecutor) {
            trObj = MatchTraceObject.create(args);
        } else if (source instanceof SpreadsheetCell) {
            SpreadsheetTracerLeaf tr = new SpreadsheetTracerLeaf((SpreadsheetCell) source);
            tr.setResult(args[0]);
            trObj = tr;
        } else if (source instanceof OpenMethodDispatcher) {
            trObj = new DTRuleTracerLeaf(((OpenMethodDispatcher) source).getCandidates().indexOf(args[0]));
        } else {
            trObj = null;
        }
        return trObj;
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

    private boolean isOn() {
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
    }
}
