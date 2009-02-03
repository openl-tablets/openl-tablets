package org.openl.rules.tbasic.compiler;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.RuntimeOperation;

public class AlgorithmCompiler {
    List<AlgorithmTreeNode> parsedNodes;
    public AlgorithmCompiler(List<AlgorithmTreeNode> parsedNodes){
        this.parsedNodes = parsedNodes;
    }
    
    private void compileNode(List<RuntimeOperation> operations, AlgorithmTreeNode node){
        //TODO use AlgorithmNodeCompilerFactory 
    }
    
    public List<RuntimeOperation> compile(){
        List<RuntimeOperation> operations = new ArrayList<RuntimeOperation>();
        for (AlgorithmTreeNode parsedNode : parsedNodes){
            compileNode(operations, parsedNode);
        }
        return operations;
    }
}
