package org.openl.rules.tbasic.compile;

import java.util.List;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.operations.PerformOperation;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

public abstract class NodeCompiler {

    List<RuntimeOperation> operations;
    AlgorithmTreeNode node;

    private RuntimeOperation compileBefore(List<RuntimeOperation> operations, AlgorithmTreeNode node){
        RuntimeOperation operation = null;
        //TODO openL statement
        //operation = new PerformOperation(String openLStatement);
        if (operation != null){
            operation.setSourceCode(node);
            operations.add(operation);
        }
        return operation;
    }
    
    private RuntimeOperation compileAfter(List<RuntimeOperation> operations, AlgorithmTreeNode node){
        RuntimeOperation operation = null;
        //TODO openL statement
        //operation = new PerformOperation(String openLStatement);
        return operation;
    }
    
   
    public NodeCompiler(List<RuntimeOperation> operations,
            AlgorithmTreeNode node) {
        super();
        this.operations = operations;
        this.node = node;
    }

    public abstract RuntimeOperation compileNode();
}
