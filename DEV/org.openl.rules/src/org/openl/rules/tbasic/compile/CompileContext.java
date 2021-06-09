package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.source.IOpenSourceCodeModule;

public class CompileContext {
    /***************************************************************************
     * Compiler output
     **************************************************************************/
    private final List<RuntimeOperation> operations;
    private final Map<String, RuntimeOperation> localLabelsRegister;
    private final Map<String, AlgorithmTreeNode> existingLables;

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

    public void registerGroupOfLabels(Map<String, AlgorithmTreeNode> labelToAdd, IBindingContext bindingContext) {
        for (Entry<String, AlgorithmTreeNode> label : labelToAdd.entrySet()) {
            registerNewLabel(label.getKey(), label.getValue(), bindingContext);
        }
    }

    public void registerNewLabel(String labelName, AlgorithmTreeNode sourceNode, IBindingContext bindingContext) {
        if (isLabelRegistered(labelName)) {
            IOpenSourceCodeModule errorSource = sourceNode.getAlgorithmRow().getOperation().asSourceCodeModule();
            BindHelper.processError("Such label has been already declared : '" + labelName + "'.",
                errorSource,
                bindingContext);
        } else {
            existingLables.put(labelName, sourceNode);
        }
    }

    public void setLabel(String labelName, RuntimeOperation labeledOperation, IBindingContext bindingContext) {
        if (isLabelRegistered(labelName)) {
            localLabelsRegister.put(labelName, labeledOperation);
        } else {
            IOpenSourceCodeModule errorSource = labeledOperation.getSourceCode().getSourceModule();
            BindHelper.processError("Such lablel is not declared : '" + labelName + "'.", errorSource, bindingContext);
        }
    }

    public void addOperations(List<RuntimeOperation> operations) {
        if (operations != null) {
            this.operations.addAll(operations);
        }
    }
}
