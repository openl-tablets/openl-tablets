package org.openl.rules.tbasic.runtime;

import java.util.List;
import java.util.Map;

import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

/**
 * The <code>TBasicVMDataContext</code> contains context for running <code>TBasicVM</code>.
 */
public class TBasicVMDataContext {
    private List<RuntimeOperation> operations;
    private Map<String, RuntimeOperation> labels;
    private boolean isMainMethodContext;

    /**
     * Create a new instance of <code>TBasicVMDataContext</code>.
     *
     * @param operations
     * @param labels
     * @param isMainMethodContext
     */
    public TBasicVMDataContext(List<RuntimeOperation> operations,
            Map<String, RuntimeOperation> labels,
            boolean isMainMethod) {
        assert operations != null;
        // assert operations.size() > 0;
        assert labels != null;

        this.operations = operations;
        this.labels = labels;
        isMainMethodContext = isMainMethod;
    }

    /**
     * Get the first operation in context to execute.
     *
     * @return The first operation.
     */
    public RuntimeOperation getFirstOperation() {
        return !operations.isEmpty() ? operations.get(0) : null;
    }

    /**
     * Get operation by its label.
     *
     * @param label The label to look for.
     * @return The labeled operation.
     */
    public RuntimeOperation getLabeledOperation(String label) {
        return labels.get(label);
    }

    /**
     * Get labels register in context.
     *
     * @return The labels register.
     */
    public Map<String, RuntimeOperation> getLabels() {
        return labels;
    }

    /**
     * Get the next operation in context after the specified.
     *
     * @param operation The current operation.
     * @return The next operation.
     */
    public RuntimeOperation getNextOperation(RuntimeOperation operation) {
        RuntimeOperation nextOperation = null;

        int indexOfCurrent = operations.indexOf(operation);

        if (indexOfCurrent > -1) {
            int indexOfNext = indexOfCurrent + 1;

            if (indexOfNext < operations.size()) {
                nextOperation = operations.get(indexOfNext);
            }
        } else {
            throw AlgorithmErrorHelper
                .createExecutionException("Cannot find the next operation after the specified one", operation);
        }

        return nextOperation;
    }

    /**
     * Get operations in context.
     *
     * @return The operations list.
     */
    public List<RuntimeOperation> getOperations() {
        return operations;
    }

    /**
     * Is the label in the context.
     *
     * @param label The label to look for.
     * @return Whether the label is in context.
     */
    public boolean isLabelInContext(String label) {
        return labels.containsKey(label);
    }

    /**
     * Get whether context is main method's one.
     *
     * @return Whether context is main method's one.
     */
    public boolean isMainMethodContext() {
        return isMainMethodContext;
    }
}