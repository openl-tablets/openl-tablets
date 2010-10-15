package org.openl.rules.tbasic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.DefaultInvokerWithTrace;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.rules.tbasic.runtime.debug.TBasicAlgorithmTraceObject;
import org.openl.types.IDynamicObject;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Invoker for {@link Algorithm}.
 * 
 * @author DLiauchuk
 *
 */
public class AlgorithmInvoker extends DefaultInvokerWithTrace {

    private final Log LOG = LogFactory.getLog(AlgorithmInvoker.class);

    private Algorithm algorithm;

    public AlgorithmInvoker(Algorithm algorithm, Object target, Object[] params, IRuntimeEnv env) {
        super(target, params, env);
        this.algorithm = algorithm;
    }

    public boolean canInvoke() {
        return algorithm.getAlgorithmSteps() != null;
    }

    public TBasicAlgorithmTraceObject createTraceObject() {        
        return new TBasicAlgorithmTraceObject(algorithm, getParams());
    }

    public OpenLRuntimeException getError() {        
        return new OpenLRuntimeException(algorithm.getSyntaxNode().getErrors()[0]);
    }

    public Object invokeSimple() {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(algorithm.getThisClass(), (IDynamicObject) getTarget());

        TBasicVM algorithmVM = new TBasicVM(algorithm.getAlgorithmSteps(), algorithm.getLabels());

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(getEnv(), thisInstance, getParams(), algorithmVM);

        return algorithmVM.run(runtimeEnvironment, false);        
    }

    public Object invokeTraced() {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(algorithm.getThisClass(), (IDynamicObject) getTarget());

        TBasicVM algorithmVM = new TBasicVM(algorithm.getAlgorithmSteps(),algorithm.getLabels());

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(getEnv(), thisInstance, getParams(), algorithmVM);

        TBasicAlgorithmTraceObject algorithmTracer = createTraceObject();
        Tracer.getTracer().push(algorithmTracer);

        Object resultValue = null;
        try {
            resultValue = algorithmVM.run(runtimeEnvironment, true);
            algorithmTracer.setResult(resultValue);
        } catch (RuntimeException e) {
            algorithmTracer.setError(e);
            LOG.error("Error when tracing TBasic table", e);
            throw e;
        } finally {
            Tracer.getTracer().pop();
        }

        return resultValue;
    }
}
