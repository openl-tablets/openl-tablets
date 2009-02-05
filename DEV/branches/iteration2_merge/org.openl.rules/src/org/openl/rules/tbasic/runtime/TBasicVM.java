package org.openl.rules.tbasic.runtime;

import java.util.List;
import java.util.Map;

import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TBasicVM {

    private List<RuntimeOperation> operations;
    private Map<String, RuntimeOperation> labels;
    
    

    public TBasicVM(List<RuntimeOperation> operations,
            Map<String, RuntimeOperation> labels) {
        // TODO: not very good that we receive 2 separate collections with the same items:
        // operations - list of all operations to execute, labels - register of (label, operation) (operation is the same as in operations)
        this.operations = operations;
        this.labels = labels;
    }

    public Object run(DelegatedDynamicObject thisTarget, Object target, Object[] params, IRuntimeEnv environment) {
        TBasicContext context = new TBasicContext(thisTarget, target, params, environment);

        RuntimeOperation operation = getFirstOperation();
        Object previousStepResult = null;
        Object returnResult = null;
        
        while (operation != null) {
            Result result = operation.execute(context, previousStepResult);

            if (result.getReturnType() == ReturnType.Goto) {
                // TODO: goto required operation
                operation = getLabeledOperation((String) result.getValue());
                continue;
            } else if (result.getReturnType() == ReturnType.Return) {
                returnResult = result.getValue();
                break;
            }
            
            operation = getNextOperation(operation);
            previousStepResult = result.getValue();
        }

        return returnResult;
    }

    /**
     * @return
     */
    private RuntimeOperation getFirstOperation() {
        return operations.get(0);
    }

    private RuntimeOperation getNextOperation(RuntimeOperation operation) {
        RuntimeOperation nextOperation = null;
        
        int indexOfNext = operations.indexOf(operation) + 1;
        
        if (indexOfNext < operations.size()){
            nextOperation = operations.get(indexOfNext);
        }
        
        return nextOperation;
    }

    private RuntimeOperation getLabeledOperation(String label) {
        return labels.get(label);
    }

}
