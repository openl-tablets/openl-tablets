package org.openl.rules.tbasic;

import java.util.List;

import org.openl.vm.IRuntimeEnv;

public class TBasicVM {

    private List<RuntimeOperation> operations;

    public Object run(Object target, Object[] params, IRuntimeEnv environment) {
        TBasicContext context = new TBasicContext(target, params, environment);

        RuntimeOperation operation = operations.get(0);
        Object previousStepResult = null;
        while (operation != null) {
            Result result = operation.execute(context, previousStepResult);

            previousStepResult = result.getValue();
            if (result.getReturnType() == ReturnType.Goto) {
                // TODO: goto required operation
                operation = getLabeledOperation((String) previousStepResult);
                continue;
            } else if (result.getReturnType() == ReturnType.Return) {
                break;
            }
            operation = getNextOperation(operation);
        }

        return previousStepResult;
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
