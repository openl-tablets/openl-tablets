package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class CompileContext {
    /***************************************************************************
     * Compiler output
     **************************************************************************/
    private List<RuntimeOperation> operations;
    private Map<String, RuntimeOperation> localLabelsRegister;
    private Map<String, AlgorithmTreeNode> existingLables;

    public CompileContext() {
        operations = new ArrayList<>();
        localLabelsRegister = new HashMap<>();
        existingLables = new HashMap<>();
    }

    public Map<String, AlgorithmTreeNode> getExistingLables() {
        return existingLables;
    }

    /**
     * @return the localLabelsRegister
     */
    public Map<String, RuntimeOperation> getLocalLabelsRegister() {
        return localLabelsRegister;
    }

    /**
     * @return the operations
     */
    public List<RuntimeOperation> getOperations() {
        return operations;
    }

    public boolean isLabelRegistered(String labelName) {
        return existingLables.containsKey(labelName);
    }

    public void registerGroupOfLabels(Map<String, AlgorithmTreeNode> labelToAdd) throws SyntaxNodeException {
        for (Entry<String, AlgorithmTreeNode> label : labelToAdd.entrySet()) {
            registerNewLabel(label.getKey(), label.getValue());
        }
    }

    /**
     *
     * @param labelName
     * @param labeledOperation
     * @throws BoundError
     */
    public void registerNewLabel(String labelName, AlgorithmTreeNode sourceNode) throws SyntaxNodeException {
        if (isLabelRegistered(labelName)) {
            IOpenSourceCodeModule errorSource = sourceNode.getAlgorithmRow().getOperation().asSourceCodeModule();
            throw SyntaxNodeExceptionUtils.createError("Such label has been already declared : '" + labelName + "'.",
                errorSource);
        } else {
            existingLables.put(labelName, sourceNode);
        }
    }

    public void setLabel(String labelName, RuntimeOperation labeledOperation) throws SyntaxNodeException {
        if (isLabelRegistered(labelName)) {
            localLabelsRegister.put(labelName, labeledOperation);
        } else {
            IOpenSourceCodeModule errorSource = labeledOperation.getSourceCode().getSourceModule();
            throw SyntaxNodeExceptionUtils.createError("Such lablel is not declared : '" + labelName + "'.",
                errorSource);
        }
    }

    public void addOperations(List<RuntimeOperation> operations) {
        if (operations != null) {
            this.operations.addAll(operations);
        }
    }
}
