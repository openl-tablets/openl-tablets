package org.openl.rules.tbasic.runtime;

import org.openl.rules.tbasic.AlgorithmTreeNode;


public abstract class RuntimeOperation {
    protected AlgorithmTreeNode sourceCode;
    
    public abstract Result execute(TBasicContextHolderEnv environment, Object param);
    
    public void setSourceCode(AlgorithmTreeNode sourceCode) {
        this.sourceCode = sourceCode;
    }
    
    /**
     * @return the sourceCode
     */
    public AlgorithmTreeNode getSourceCode() {
        return sourceCode;
    }
}
