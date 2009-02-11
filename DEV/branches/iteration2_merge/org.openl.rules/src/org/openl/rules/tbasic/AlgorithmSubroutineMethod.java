/**
 * 
 */
package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.rules.tbasic.runtime.RuntimeOperation;
import org.openl.rules.tbasic.runtime.TBasicContext;
import org.openl.rules.tbasic.runtime.TBasicEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author User
 *
 */
public class AlgorithmSubroutineMethod extends AMethod implements IOpenMethod {

    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;
    
    public AlgorithmSubroutineMethod(IOpenMethodHeader header) {
        super(header);
    }

    /* (non-Javadoc)
     * @see org.openl.types.IMethodCaller#invoke(java.lang.Object, java.lang.Object[], org.openl.vm.IRuntimeEnv)
     */
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        assert env instanceof TBasicEnv;
        
        TBasicEnv runtimeEnvironment = (TBasicEnv) env;
        TBasicVM vm = runtimeEnvironment.getTbasicVm();
        TBasicContext context = new TBasicContext(target, params, runtimeEnvironment);
        
        return vm.run(algorithmSteps, labels, context);
    }


    public void setAlgorithmSteps(List<RuntimeOperation> operations) {
        algorithmSteps = operations;
        
    }

    public void setLabels(Map<String, RuntimeOperation> localLabelsRegister) {
        labels = localLabelsRegister;
        
    }

}
