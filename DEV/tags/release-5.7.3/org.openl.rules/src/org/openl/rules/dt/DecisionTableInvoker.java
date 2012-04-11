package org.openl.rules.dt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.domain.IIntIterator;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.algorithm.FailOnMissException;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link DecisionTable}.
 * 
 * @author DLiauchuk
 *
 */
public class DecisionTableInvoker extends DefaultInvokerWithTrace {
    
    private final Log LOG = LogFactory.getLog(DecisionTableInvoker.class);
    
    private DecisionTable decisionTable;
    
    public DecisionTableInvoker(DecisionTable decisionTable) {        
        this.decisionTable = decisionTable;
    }
    
    public boolean canInvoke() {
        return decisionTable.getAlgorithm() != null;
    }
    
    private Object invokeOptimized(Object target, Object[] params, IRuntimeEnv env) {

        IIntIterator rules = decisionTable.getAlgorithm().checkedRules(target, params, env);

        Object returnValue = null;
        boolean atLeastOneRuleFired = false;

        while (rules.hasNext()) {

            atLeastOneRuleFired = true;
            int ruleN = rules.nextInt();

            for (int j = 0; j < decisionTable.getActionRows().length; j++) {

                Object actionResult = decisionTable.getActionRows()[j].executeAction(ruleN, target, params, env);

                if (decisionTable.getActionRows()[j].isReturnAction() && returnValue == null && (actionResult != null || (decisionTable.getActionRows()[j].getParamValues()!= null && decisionTable.getActionRows()[j].getParamValues()[ruleN] != null))) {
                    returnValue = actionResult;
                }
            }
            if (returnValue != null) {
                return returnValue;
            }
        }

        if (!atLeastOneRuleFired && decisionTable.shouldFailOnMiss()) {

            String method = MethodUtil.printMethodWithParameterValues(decisionTable.getMethod(), params, INamedThing.REGULAR);
            String message = String.format("%s failed to match any rule condition", method);

            throw new FailOnMissException(message, decisionTable, params);
        }

        return returnValue;
    }
    
    private Object invokeTracedOptimized(Object target, Object[] params, IRuntimeEnv env) {        
        Tracer tracer = Tracer.getTracer();
        
        if (tracer == null) {
            return invokeOptimized(target, params, env);
        }

        Object ret = null;

        DecisionTableTraceObject traceObject = new DecisionTableTraceObject(decisionTable, params);
        tracer.push(traceObject);

        try {
            IIntIterator rules = decisionTable.getAlgorithm().checkedRules(target, params, env);

            while (rules.hasNext()) {

                int ruleN = rules.nextInt();

                try {
                    tracer.push(traceObject.traceRule(ruleN));

                    for (int j = 0; j < decisionTable.getActionRows().length; j++) {
                        Object actionResult = decisionTable.getActionRows()[j].executeAction(ruleN, target, params, env);

                        if (decisionTable.getActionRows()[j].isReturnAction() && ret == null
                                && (actionResult != null || (decisionTable.getActionRows()[j].getParamValues()!= null
                                        && decisionTable.getActionRows()[j].getParamValues()[ruleN] != null))) {
                            ret = actionResult;
                        }
                    }
                    if (ret != null) {
                        traceObject.setResult(ret);
                        return ret;
                    }
                } finally {
                    tracer.pop();
                }
            }
        } catch (RuntimeException e) {
            addErrorToTrace(traceObject, e);
        } finally {
            tracer.pop();
        }

        return ret;
    }
    
    private void addErrorToTrace(DecisionTableTraceObject traceObject, Throwable e) {
        traceObject.setError(e);
        LOG.error("Error when tracing DT rule", e);
        throw new OpenLRuntimeException(e);
    }
    
    public DecisionTableTraceObject createTraceObject(Object[] params) {
        return new DecisionTableTraceObject(decisionTable, params);
    }
    
    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        return invokeTracedOptimized(target, params, env);
    }
    
    public OpenLRuntimeException getError() {
        return new OpenLRuntimeException(decisionTable.getSyntaxNode().getErrors()[0]);
    }
    
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        return invokeOptimized(target, params, env);
    }

}
