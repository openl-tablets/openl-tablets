package org.openl.rules.tbasic.compiler;

import java.util.List;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.ReturnOperation;
import org.openl.rules.tbasic.runtime.RuntimeOperation;

public class AlgorithmNodeCompilerFactory {
    public static NodeCompiler getNodeCompiler(List<RuntimeOperation> operations,
            AlgorithmTreeNode node){
        NodeCompiler compiler = null;
        if (node.getSpecification().getKeyword().equals("GOTO")){
            compiler = new GotoCompiler(operations, node);
        }else if (node.getSpecification().getKeyword().equals("RETURN")){
            RuntimeOperation operation = null;
            if (node.getAlgorithmRow().getAction() == null){
                operation = new ReturnOperation<Integer>();
            }else{
                //TODO openL statement 
                //operation = new ReturnOperation<Integer>(String openLStatement);
            }
            operation.setSourceCode(node);
            operations.add(operation);
        }
        return compiler;
    }
}
