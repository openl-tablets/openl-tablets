/**
 * Created Dec 3, 2006
 */
package org.openl.rules.webstudio.web.trace;

import org.openl.rules.cmatch.algorithm.MatchAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.ScoreAlgorithmExecutor;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmExecutor;
import org.openl.rules.dt.algorithm.DecisionTableOptimizedAlgorithm;
import org.openl.rules.webstudio.web.trace.node.DTRuleTraceObject;
import org.openl.rules.webstudio.web.trace.node.MatchTraceObject;
import org.openl.rules.webstudio.web.trace.node.ResultTraceObject;
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
    protected void doPut(ITracerObject obj) {
        ITracerObject current = tree.get();
        current.addChild(obj);
    }

    @Override
    protected void doPut(Object executor, String id, Object... args) {
        if (executor instanceof DecisionTableOptimizedAlgorithm) {
            doPut(DTRuleTraceObject.create(args));
        } else if (executor instanceof MatchAlgorithmExecutor) {
            if ("match".equals(id)) {
                doPut(MatchTraceObject.create(args));
            } else {
                doPut(ResultTraceObject.create(args));
            }
        } else if (executor instanceof WeightAlgorithmExecutor) {
            if ("match".equals(id)) {
                doPut(MatchTraceObject.create(args));
            } else {
                doPut(ResultTraceObject.create(args));
            }
        } else if (executor instanceof ScoreAlgorithmExecutor) {
            doPut(MatchTraceObject.create(args));
        }
    }

    @Override
    protected void doBegin(ITracerObject obj) {
        doPut(obj);
        tree.set(obj);
    }

    @Override
    protected void doEnd() {
        ITracerObject current = tree.get();
        if (current != null) {
            tree.set(current.getParent());
        } else {
            log.warn("Something is wrong. Current trace object is null. Can't pop trace object.");
        }
    }

    @Override
    protected boolean isOn() {
        return true;
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
