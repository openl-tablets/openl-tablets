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
public class AlgorithmInvoker extends RulesMethodInvoker<Algorithm> {

    public AlgorithmInvoker(Algorithm algorithm) {
        super(algorithm);
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithmSteps() != null;
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(getInvokableMethod().getThisClass(),
            (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(getInvokableMethod().getType(),
            getInvokableMethod().getAlgorithmSteps(),
            getInvokableMethod().getLabels());

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        return algorithmVM.run(runtimeEnvironment);
    }

}
