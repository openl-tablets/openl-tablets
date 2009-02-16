package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * 
 */
public abstract class RuntimeOperation {
    private AlgorithmTreeNode sourceCode;
    
    public abstract Result execute(TBasicContextHolderEnv environment, Object param);
    
    public void setSourceCode(AlgorithmTreeNode sourceCode) {
        this.sourceCode = sourceCode;
    }
    
    /**
     * @return the source code
     */
    public AlgorithmTreeNode getSourceCode() {
        return sourceCode;
    }
}
