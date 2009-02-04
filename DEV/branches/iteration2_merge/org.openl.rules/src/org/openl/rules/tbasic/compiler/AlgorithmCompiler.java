package org.openl.rules.tbasic.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.RuntimeOperation;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DelegatedDynamicObject;

public class AlgorithmCompiler {
    /********************************
     *  Initial data
     *******************************/
    private OpenL openl;
    private IOpenMethodHeader header;
    private List<AlgorithmTreeNode> parsedNodes;
    
    /*********************************
     * Compiler output
     ********************************/
    private List<RuntimeOperation> operations;
    private IOpenClass thisTarget;
    
    public AlgorithmCompiler(OpenL openl, IOpenMethodHeader header, List<AlgorithmTreeNode> parsedNodes){
        this (openl, header, parsedNodes, true);
    }
    
    public AlgorithmCompiler(OpenL openl, IOpenMethodHeader header, List<AlgorithmTreeNode> parsedNodes, boolean compileImmediately){
        this.openl = openl;
        this.header = header;
        this.parsedNodes = parsedNodes;
        if (compileImmediately) {
            compile();
        }
    }
    
    public void compile(){
        operations = new ArrayList<RuntimeOperation>();
        thisTarget = new ModuleOpenClass(null, generateOpenClassName(), openl); 
            
        preProcess();
    }

    private String generateOpenClassName() {
        return header.getName();
    }

    /**
     * 
     */
    private void preProcess() {
//        for (int i = 0; i < parsedNodes.size(); i++){
//            AlgorithmTreeNode parsedNode = parsedNodes.get(i);
//            ArrayList operationsToGroupWithCurrent;// = Collection.; new ArrayList<AlgorithmTreeNode>(new String[] {});
//            int shiftToNextToGroupOperation = 1;
//            for (;shiftToNextToGroupOperation < parsedNodes.size() - i; shiftToNextToGroupOperation++){
//                AlgorithmTreeNode groupCandidateNode = parsedNodes.get(i + shiftToNextToGroupOperation);
//                if (!operationsToGroupWithCurrent.contains(groupCandidateNode.getSpecification().getKeyword())){
//                    break;
//                }
//            }
//        }
    }
}
