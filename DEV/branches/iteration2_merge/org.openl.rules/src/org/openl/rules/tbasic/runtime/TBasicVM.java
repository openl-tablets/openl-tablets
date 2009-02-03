package org.openl.rules.tbasic.runtime;

import java.util.List;

import org.openl.vm.IRuntimeEnv;

public class TBasicVM {

    private List<RuntimeOperation> operations;

    public Object run(Object target, Object[] params, IRuntimeEnv environment) {
        TBasicContext context = new TBasicContext(target, params, environment);

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
        return null;
    }

    private RuntimeOperation getLabeledOperation(String previousStepResult) {
        // TODO Auto-generated method stub
        return null;
    }

}
