package org.openl.rules.tbasic;

import org.openl.rules.method.RulesMethodInvoker;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.types.IDynamicObject;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoker for {@link Algorithm}.
 *
 * @author DLiauchuk
 */
public class AlgorithmInvoker extends RulesMethodInvoker {

    private final Logger log = LoggerFactory.getLogger(AlgorithmInvoker.class);

    public AlgorithmInvoker(Algorithm algorithm) {
        super(algorithm);
    }

    @Override
    public Algorithm getInvokableMethod() {
        return (Algorithm) super.getInvokableMethod();
    }

    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithmSteps() != null;
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(getInvokableMethod().getThisClass(), (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(getInvokableMethod().getType(), getInvokableMethod().getAlgorithmSteps(), getInvokableMethod().getLabels());

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        return algorithmVM.run(runtimeEnvironment, false);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(getInvokableMethod().getThisClass(), (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(getInvokableMethod().getType(), getInvokableMethod().getAlgorithmSteps(), getInvokableMethod().getLabels());

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        ATableTracerNode algorithmTracer = getTraceObject(params);
        Tracer.getTracer().push(algorithmTracer);

        Object resultValue = null;
        try {
            resultValue = algorithmVM.run(runtimeEnvironment, true);
            algorithmTracer.setResult(resultValue);
        } catch (RuntimeException e) {
            algorithmTracer.setError(e);
            log.error("Error when tracing TBasic table", e);
            throw e;
        } finally {
            Tracer.getTracer().pop();
        }

        return resultValue;
    }
}
