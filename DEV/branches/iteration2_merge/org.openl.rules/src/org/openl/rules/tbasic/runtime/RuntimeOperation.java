package org.openl.rules.tbasic.runtime;

import org.openl.rules.tbasic.AlgorithmTreeNode;


public abstract class RuntimeOperation {
    protected AlgorithmTreeNode sourceCode;
    public abstract Result execute(TBasicContext context, Object param);
    public void setSourceCode(AlgorithmTreeNode sourceCode) {
        this.sourceCode = sourceCode;
    }
}
