package org.openl.rules.tbasic.compiler;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.RuntimeOperation;

public class AlgorithmCompiler {
    /********************************
     *  Initial data
     *******************************/
    private List<AlgorithmTreeNode> parsedNodes;
    
    /*********************************
     * Compiler output
     ********************************/
    private List<RuntimeOperation> operations;
    
    public AlgorithmCompiler(List<AlgorithmTreeNode> parsedNodes){
        this (parsedNodes, true);
    }
    
    public AlgorithmCompiler(List<AlgorithmTreeNode> parsedNodes, boolean compileImmediately){
        this.parsedNodes = parsedNodes;
        if (compileImmediately) {
            compile();
        }
    }
    
    private void compileNode(List<RuntimeOperation> operations, AlgorithmTreeNode node){
        //TODO use AlgorithmNodeCompilerFactory 
    }
    
    public void compile(){
        operations = new ArrayList<RuntimeOperation>();
        
        for (AlgorithmTreeNode parsedNode : parsedNodes){
            compileNode(operations, parsedNode);
        }
    }
}
