package org.openl.rules.tbasic.runtime;

import java.util.List;
import java.util.Map;

import org.openl.binding.impl.ControlSignal;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

// TODO: refactore
public class TBasicVM {

    private List<RuntimeOperation> mainOperations;
    private Map<String, RuntimeOperation> mainLabels;

    private List<RuntimeOperation> currentOperations;
    private Map<String, RuntimeOperation> currentLabels;
    private boolean isCurrentMainMethod;

    public TBasicVM(List<RuntimeOperation> operations, Map<String, RuntimeOperation> labels) {
        // TODO: not very good that we receive 2 separate collections with the
        // same items:
        // operations - list of all operations to execute, labels - register of
        // (label, operation) (operation is the same as in operations)
        mainOperations = operations;
        mainLabels = labels;
        
        // in the first turn only main can be called
        currentOperations = mainOperations;
        currentLabels = mainLabels;
        isCurrentMainMethod = true; 
    }

    public Object run(TBasicContextHolderEnv environment) {
        assert environment != null;

        Object returnResult = null;
        
        // Run fail safe, in case of error allow user code to handle it
        // processing of error will be done in Algorithm main method
        try {
            returnResult = runAll(environment);
        } catch (OpenLAlgorithmErrorSignal signal){
            if (isCurrentMainMethod) {
                returnResult = AlgorithmErrorHelper.processError(signal.getCause(), environment);
            } else {
                throw signal;
            }
        } catch (ControlSignal signal){
            throw signal;
        } catch (Throwable error){
            if (isCurrentMainMethod) {
                returnResult = AlgorithmErrorHelper.processError(error, environment);
            } else {
                throw new OpenLAlgorithmErrorSignal(error);
            }
        }

        return returnResult;
    }

    public Object run(List<RuntimeOperation> methodSteps, Map<String, RuntimeOperation> methodLabels,
            TBasicContextHolderEnv environment) {
        
        List<RuntimeOperation> previousOperations = swapOperations(methodSteps);
        Map<String, RuntimeOperation> previousLabels = swapLabels(methodLabels);
        boolean previousIsMainMethod = swapIsMainMethod(false);

        try {
            return run(environment);
        } finally {
            swapOperations(previousOperations);
            swapLabels(previousLabels);
            swapIsMainMethod(previousIsMainMethod);
        }
    }
    
    /**
     * @param environment
     * @return
     */
    private Object runAll(TBasicContextHolderEnv environment) {
        RuntimeOperation operation = getFirstOperation();
        Object previousStepResult = null;
        Object returnResult = null;
        
        while (operation != null) {
            Result operationResult;
            try {
                operationResult = operation.execute(environment, previousStepResult);
                assert operationResult != null;
            } catch (OpenLAlgorithmGoToMainSignal signal){
                operation = getLabeledOperation(signal.getLabel());
                continue;
            }
            
            if (operationResult.getReturnType() == ReturnType.Goto) {
                assert operationResult.getValue() instanceof String;
                operation = getLabeledOperation((String) operationResult.getValue());
                continue;
            } else if (operationResult.getReturnType() == ReturnType.Return) {
                returnResult = operationResult.getValue();
                break;
            }
   
            operation = getNextOperation(operation);
            previousStepResult = operationResult.getValue();
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

    /**
     * @param newOperations
     */
    private List<RuntimeOperation> swapOperations(List<RuntimeOperation> newOperations) {
        List<RuntimeOperation> oldValue = currentOperations;
        currentOperations = newOperations;
        return oldValue;
    }
    
    /**
     * @param newLabels
     */
    private Map<String, RuntimeOperation> swapLabels(Map<String, RuntimeOperation> newLabels) {
        Map<String, RuntimeOperation> oldValue = currentLabels;
        currentLabels = newLabels;
        return oldValue;
    }
    
    /**
     * @param newIsMainMethod
     */
    private boolean swapIsMainMethod(boolean newIsMainMethod) {
        boolean oldValue = isCurrentMainMethod;
        isCurrentMainMethod = newIsMainMethod;
        return oldValue;
    }
}
