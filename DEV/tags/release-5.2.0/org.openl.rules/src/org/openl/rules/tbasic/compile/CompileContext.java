package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.IOpenSourceCodeModule;
import org.openl.binding.impl.BoundError;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

public class CompileContext {
    /*********************************
     * Compiler output
     ********************************/
    private List<RuntimeOperation> operations;
    private Map<String, RuntimeOperation> localLabelsRegister;
    private Map<String, AlgorithmTreeNode> existingLables;
    
    public CompileContext(){
        operations = new ArrayList<RuntimeOperation>();
        localLabelsRegister = new HashMap<String, RuntimeOperation>();
        existingLables = new HashMap<String, AlgorithmTreeNode>();
    }
    
    /**
     * @return the operations
     */
    public List<RuntimeOperation> getOperations() {
        return operations;
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
     * 
     * @param labelName
     * @param labeledOperation
     * @throws BoundError
     */
    public void registerNewLabel(String labelName, AlgorithmTreeNode sourceNode) throws BoundError {
        if (isLabelRegistered(labelName)) {
            IOpenSourceCodeModule errorSource = sourceNode.getAlgorithmRow().getOperation().asSourceCodeModule();
            throw new BoundError("Such label has been already declared : \"" + labelName + "\".", errorSource);
        } else {
            existingLables.put(labelName, sourceNode);
        }
    }

    public void setLabel(String labelName, RuntimeOperation labeledOperation) throws BoundError {
        if (isLabelRegistered(labelName)) {
            localLabelsRegister.put(labelName, labeledOperation);
        } else {
            IOpenSourceCodeModule errorSource = labeledOperation.getSourceCode().getSourceModule();
            throw new BoundError("Such lablel isn't declared : \"" + labelName + "\".", errorSource);
        }
    }
    
    public boolean isLabelRegistered(String labelName){
        return existingLables.containsKey(labelName);
    }

    public void registerGroupOfLabels(Map<String, AlgorithmTreeNode> labelToAdd) throws BoundError {
        for (Entry<String, AlgorithmTreeNode> label : labelToAdd.entrySet()) {
            registerNewLabel(label.getKey(), label.getValue());
        }
    }
}
