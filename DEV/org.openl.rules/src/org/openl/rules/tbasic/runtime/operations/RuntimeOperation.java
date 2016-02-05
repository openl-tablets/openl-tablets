package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.compile.AlgorithmOperationSource;
import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;

/**
 * The <code>RuntimeOperation</code> class describes operation in common.
 * Contains link to source of this operation and can be executed with debug
 * mode.
 *
 */
public abstract class RuntimeOperation {
    private AlgorithmOperationSource sourceCode;

    private String nameForDebug;

    /**
     * Run operation in specified context.
     *
     * @param environment Environment for execution.
     * @param param Argument for execution.
     * @return The result of operation
     */
    public abstract Result execute(TBasicContextHolderEnv environment, Object param);

    /**
     * @return the nameForDebug
     */
    public String getNameForDebug() {
        return nameForDebug;
    }

    /**
     * @return the source code
     */
    public AlgorithmOperationSource getSourceCode() {
        return sourceCode;
    }

    /**
     * @param nameForDebug the nameForDebug to set
     */
    public void setNameForDebug(String nameForDebug) {
        this.nameForDebug = nameForDebug;
    }

    public void setSourceCode(AlgorithmOperationSource sourceCode) {
        this.sourceCode = sourceCode;
    }
}
