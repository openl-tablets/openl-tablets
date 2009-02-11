package org.openl.rules.tbasic.runtime;

import java.util.List;
import java.util.Map;

import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TBasicVM {

    private List<RuntimeOperation> mainOperations;
    private Map<String, RuntimeOperation> mainLabels;

    private List<RuntimeOperation> currentOperations;
    private Map<String, RuntimeOperation> currentLabels;

    public TBasicVM(List<RuntimeOperation> operations, Map<String, RuntimeOperation> labels) {
        // TODO: not very good that we receive 2 separate collections with the
        // same items:
        // operations - list of all operations to execute, labels - register of
        // (label, operation) (operation is the same as in operations)
        mainOperations = operations;
        mainLabels = labels;
        
        currentOperations = mainOperations;
        currentLabels = mainLabels;
    }

    public Object run(TBasicContext context) {

        RuntimeOperation operation = getFirstOperation();
        Object previousStepResult = null;
        Object returnResult = null;

        while (operation != null) {
            Result result = null;
            try {
                result = operation.execute(context, previousStepResult);
            } catch (OpenLAlgorithmGoToMainSignal signal){
                operation = getLabeledOperation(signal.getLabel());
                continue;
            }

            // TODO: result == null
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
        return currentOperations.get(0);
    }

    private RuntimeOperation getNextOperation(RuntimeOperation operation) {
        RuntimeOperation nextOperation = null;

        int indexOfNext = currentOperations.indexOf(operation) + 1;

        if (indexOfNext < currentOperations.size()) {
            nextOperation = currentOperations.get(indexOfNext);
        }

        return nextOperation;
    }

    private RuntimeOperation getLabeledOperation(String label) {
        if (currentLabels.containsKey(label)) {
            return currentLabels.get(label);
        } else if (mainLabels.containsKey(label)) {
            returnBackToMain(label);
        }
        throw new RuntimeException(String.format(
                "Unexpected error while execution of TBasic component: unknown label \"%s\"", label));
    }

    private void returnBackToMain(String label) {
        throw new OpenLAlgorithmGoToMainSignal(label);
    }

    public Object run(List<RuntimeOperation> methodSteps, Map<String, RuntimeOperation> methodLabels,
            TBasicContext context) {
        List<RuntimeOperation> previousOperations = swapOperations(methodSteps);
        Map<String, RuntimeOperation> previousLabels = swapLabels(methodLabels);

        try {
            Object result = run(context);
            return result;
        } finally {
            swapOperations(previousOperations);
            swapLabels(previousLabels);
        }
    }

    /**
     * @param newOperations
     */
    private List<RuntimeOperation> swapOperations(List<RuntimeOperation> newOperations) {
        List<RuntimeOperation> oldOperations = currentOperations;
        currentOperations = newOperations;
        return oldOperations;
    }

    /**
     * @param newLabels
     */
    private Map<String, RuntimeOperation> swapLabels(Map<String, RuntimeOperation> newLabels) {
        Map<String, RuntimeOperation> oldOperations = currentLabels;
        currentLabels = newLabels;
        return oldOperations;
    }

}
