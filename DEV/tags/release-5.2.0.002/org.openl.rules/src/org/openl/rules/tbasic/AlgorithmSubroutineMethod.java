/**
 * 
 */
package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Table Basic Algorithm component for internal subroutines and functions. 
 * It can be run only inside call hierarchy of parent Algorithm. 
 * However, it doesn't have any links to parent Algorithm, but relies on invocation arguments.
 */
public class AlgorithmSubroutineMethod extends AMethod implements IOpenMethod {

    /*******************************************
     * Compile artifacts
     ******************************************/
    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;
    
    public AlgorithmSubroutineMethod(IOpenMethodHeader header) {
        super(header);
    }

    /* (non-Javadoc)
     * @see org.openl.types.IMethodCaller#invoke(java.lang.Object, java.lang.Object[], org.openl.vm.IRuntimeEnv)
     */
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        assert env instanceof TBasicContextHolderEnv;
        
        TBasicContextHolderEnv environment = (TBasicContextHolderEnv) env;
        TBasicVM vm = environment.getTbasicVm();
         
        return vm.run(algorithmSteps, labels, environment);
    }


    public void setAlgorithmSteps(List<RuntimeOperation> operations) {
        algorithmSteps = operations;
        
    }

    public void setLabels(Map<String, RuntimeOperation> localLabelsRegister) {
        labels = localLabelsRegister;
        
    }

}
