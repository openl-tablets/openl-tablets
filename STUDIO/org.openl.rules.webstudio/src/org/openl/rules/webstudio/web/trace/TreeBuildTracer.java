/**
 * Created Dec 3, 2006
 */
package org.openl.rules.webstudio.web.trace;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.webstudio.web.trace.node.ColumnMatchTraceObject;
import org.openl.rules.webstudio.web.trace.node.SpreadsheetTracerLeaf;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.algorithm.MatchAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.ScoreAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.WScoreTraceObject;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.webstudio.web.trace.node.DTRuleTracerLeaf;
import org.openl.rules.webstudio.web.trace.node.TracedObjectFactory;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.webstudio.web.trace.node.OverloadedMethodChoiceTraceObject;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.MatchTraceObject;
import org.openl.rules.webstudio.web.trace.node.ResultTraceObject;
import org.openl.rules.webstudio.web.trace.node.WColumnMatchTraceObject;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.SimpleTracerObject;
import org.openl.vm.trace.Tracer;
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
        if (source instanceof DecisionTableOptimizedAlgorithm) {
            doPut(DTRuleTraceObject.create(args));
        } else if (source instanceof MatchAlgorithmExecutor) {
            if ("match".equals(id)) {
                doPut(MatchTraceObject.create(args));
            } else {
                doPut(ResultTraceObject.create(args));
            }
        } else if (source instanceof WeightAlgorithmExecutor) {
            if ("match".equals(id)) {
                doPut(MatchTraceObject.create(args));
            } else {
                doPut(ResultTraceObject.create(args));
            }
        } else if (source instanceof ScoreAlgorithmExecutor) {
            doPut(MatchTraceObject.create(args));
        } else if (source instanceof SpreadsheetCell) {
            SpreadsheetTracerLeaf tr = new SpreadsheetTracerLeaf((SpreadsheetCell) source);
            tr.setResult(args[0]);
            doPut(tr);
        } else if (source instanceof OpenMethodDispatcher) {
            doPut(new DTRuleTracerLeaf(((OpenMethodDispatcher) source).getCandidates().indexOf(args[0])));
        }
    }

    private void doPut(ITracerObject obj) {
        ITracerObject current = tree.get();
        current.addChild(obj);
    }

    @Override
    protected void doBegin(ITracerObject obj) {
        if (!isOn()) {
            return;
        }
        doPut(obj);
        tree.set(obj);
    }

    @Override
    protected void doEnd() {
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
    protected <T> Object doInvoke(Invokable<? super T> executor, T target, Object[] params, IRuntimeEnv env, Object source) {
        if (!isOn()) {
            // Skip if tracing is switched off
            return executor.invoke(target, params, env);
        }
        ATableTracerNode trObj;
        if (source instanceof OpenMethodDispatcher) {
            trObj = OverloadedMethodChoiceTraceObject.create((OpenMethodDispatcher) source, params);
        } else if (source instanceof WeightAlgorithmExecutor) {
            trObj = new WScoreTraceObject((ColumnMatch) target, params);
        } else if (source instanceof ColumnMatch) {
            ColumnMatch columnMatch = (ColumnMatch) source;
            if (executor instanceof WeightAlgorithmExecutor) {
                trObj = new WColumnMatchTraceObject(columnMatch, params);
            } else {
                trObj = new ColumnMatchTraceObject(columnMatch, params);
            }
        } else {
            trObj = TracedObjectFactory.getTracedObject(executor, params);
        }
        if (trObj == null) {
            // Skip if no tracing objects are
            return executor.invoke(target, params, env);
        }
        doBegin(trObj);
        try {
            Object res = executor.invoke(target, params, env);
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
    protected boolean isOn() {
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
