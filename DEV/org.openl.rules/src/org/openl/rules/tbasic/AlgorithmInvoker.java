package org.openl.rules.tbasic;

import org.openl.rules.method.RulesMethodInvoker;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.types.IDynamicObject;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker for {@link Algorithm}.
 *
 * @author Yury Molchan
 */
public class AlgorithmInvoker extends RulesMethodInvoker {

    public AlgorithmInvoker(Algorithm algorithm) {
        super(algorithm);
    }

    @Override
    public Algorithm getInvokableMethod() {
        return (Algorithm) super.getInvokableMethod();
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithmSteps() != null;
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        return invoke((IDynamicObject) target, params, env, false);
    }

    @Override
    protected Object invokeSimpleTraced(Object target, Object[] params, IRuntimeEnv env) {
        return invoke((IDynamicObject) target, params, env, true);
    }

    private Object invoke(IDynamicObject target, Object[] params, IRuntimeEnv env, boolean debugMode) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(getInvokableMethod().getThisClass(), target);

        TBasicVM algorithmVM = new TBasicVM(getInvokableMethod().getType(), getInvokableMethod().getAlgorithmSteps(), getInvokableMethod().getLabels());

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        return algorithmVM.run(runtimeEnvironment, debugMode);
    }
}
