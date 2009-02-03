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
        // TODO Auto-generated method stub
        int indexOfNext = operations.indexOf(operation)+1;
        return operations.get(indexOfNext);
    }

    private RuntimeOperation getLabeledOperation(String label) {
        return labels.get(label);
    }

}
