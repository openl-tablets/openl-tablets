package org.openl.rules.tbasic.runtime;

import java.util.List;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public class TBasicVM {

    private List<RuntimeOperation> mainOperations;
    private Map<String, RuntimeOperation> mainLabels;

    private List<RuntimeOperation> currentOperations;
    private Map<String, RuntimeOperation> currentLabels;
    
    private boolean isMainMethod;

    public TBasicVM(List<RuntimeOperation> operations, Map<String, RuntimeOperation> labels) {
        // TODO: not very good that we receive 2 separate collections with the
        // same items:
        // operations - list of all operations to execute, labels - register of
        // (label, operation) (operation is the same as in operations)
        mainOperations = operations;
        mainLabels = labels;
        
        currentOperations = mainOperations;
        currentLabels = mainLabels;
        isMainMethod = true; // in the first turn only main can be called
    }

    public Object run(TBasicContextHolderEnv environment) {

        RuntimeOperation operation = getFirstOperation();
        Object previousStepResult = null;
        Object returnResult = null;

        try {
            while (operation != null) {
                Result result = null;
                try {
                    result = operation.execute(environment, previousStepResult);
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
        } catch (OpenLAlgorithmErrorSignal signal){
            if (!isMainMethod) {
                throw signal;
            } else if (isMainMethod){
                // TODO discover which exception contains exception
                returnResult = processError(signal.getCause().getCause(), environment);
            }
        } catch (Throwable error){
            if (!isMainMethod) {
                throw new OpenLAlgorithmErrorSignal(error);
            } else if (isMainMethod){
                // TODO discover which exception contains exception
                returnResult = processError(error.getCause(), environment);
            }
        }

        return returnResult;
    }

    private Object processError(Throwable signal, TBasicContextHolderEnv environment) {
        IOpenClass algorithmType = environment.getTbasicTarget().getType();
        IOpenMethod errorMethod = algorithmType.getMethod("ON ERROR", new IOpenClass[]{});
        
        if (errorMethod != null){
            IOpenField error = algorithmType.getField("ERROR");
            if (error != null){
                // populate error messages
                error.set(environment.getTbasicTarget(), signal, environment);
            }
            return errorMethod.invoke(environment.getTbasicTarget(), null, environment);
        }
        
        throw new RuntimeException(String.format("Execution of algorithm failed: %s", signal.getMessage()), signal);
    }

    public Object run(List<RuntimeOperation> methodSteps, Map<String, RuntimeOperation> methodLabels,
            TBasicContextHolderEnv environment) {
        
        List<RuntimeOperation> previousOperations = swapOperations(methodSteps);
        Map<String, RuntimeOperation> previousLabels = swapLabels(methodLabels);
        boolean previousIsMainMethod = swapIsMainMethod(false);

        try {
            Object result = run(environment);
            return result;
        } finally {
            swapOperations(previousOperations);
            swapLabels(previousLabels);
            swapIsMainMethod(previousIsMainMethod);
        }
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
        boolean oldValue = isMainMethod;
        isMainMethod = newIsMainMethod;
        return oldValue;
    }

}
