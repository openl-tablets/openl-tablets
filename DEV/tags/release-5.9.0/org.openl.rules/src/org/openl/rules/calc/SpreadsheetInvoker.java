package org.openl.rules.calc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link Spreadsheet}.
 * 
 * @author DLiauchuk
 *
 */
public class SpreadsheetInvoker extends RulesMethodInvoker {

    private final Log LOG = LogFactory.getLog(SpreadsheetInvoker.class);

    public SpreadsheetInvoker(Spreadsheet spreadsheet) {
        super(spreadsheet);
    }
    
    @Override
    public Spreadsheet getInvokableMethod() {    
        return (Spreadsheet)super.getInvokableMethod();
    }

    public boolean canInvoke() {        
        return getInvokableMethod().getResultBuilder() != null;
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {        
      SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(getInvokableMethod(), (IDynamicObject) target, params, env);
      return getInvokableMethod().getResultBuilder().makeResult(res);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        Object result = null;

        SpreadsheetTraceObject traceObject = (SpreadsheetTraceObject)getTraceObject(params);
        tracer.push(traceObject);

        try {
            SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(getInvokableMethod(), (IDynamicObject) target, params,
                    env, traceObject);

            result = getInvokableMethod().getResultBuilder().makeResult(res);
            traceObject.setResult(result);
        } catch (RuntimeException e) {
           traceObject.setError(e);
           LOG.error("Error when tracing Spreadsheet table", e);
           throw e;
        } finally {
            tracer.pop();
        }
        return result;
    }
}
