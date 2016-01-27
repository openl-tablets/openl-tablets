package org.openl.rules.tbasic.runtime.operations;

import java.util.HashMap;

import org.openl.rules.tbasic.compile.AlgorithmOperationSource;
import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.debug.TBasicOperationTraceObject;
import org.openl.vm.trace.Tracer;

/**
 * The <code>RuntimeOperation</code> class describes operation in common.
 * Contains link to source of this operation and can be executed with debug
 * mode.
 *
 */
public abstract class RuntimeOperation {
    private AlgorithmOperationSource sourceCode;

    private boolean significantForDebug;
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
     * Execute operation in debug mode if necessary.
     *
     * @param environment Environment for execution.
     * @param param Argument for execution.
     * @param debugMode Flag of necessity of debugging.
     * @return The result of operation
     */
    public Result execute(TBasicContextHolderEnv environment, Object param, boolean debugMode) {
        Result result = null;

        TBasicOperationTraceObject operationTracer = null;

        if (debugMode && significantForDebug) {
            operationTracer = new TBasicOperationTraceObject(getSourceCode(), getNameForDebug());
            operationTracer.setFieldValues((HashMap<String, Object>)environment.getTbasicTarget().getFieldValues());
            Tracer.begin(operationTracer);
        }
        try {
            result = execute(environment, param);
            if (debugMode && significantForDebug) {
                operationTracer.setResult(result.getValue());
            }
        } finally {
            if (debugMode && significantForDebug) {
                Tracer.end();
            }
        }
        return result;
    }

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
     * @return the significantForDebug
     */
    public boolean isSignificantForDebug() {
        return significantForDebug;
    }

    /**
     * @param nameForDebug the nameForDebug to set
     */
    public void setNameForDebug(String nameForDebug) {
        this.nameForDebug = nameForDebug;
    }

    /**
     * @param significantForDebug the significantForDebug to set
     */
    public void setSignificantForDebug(boolean significantForDebug) {
        this.significantForDebug = significantForDebug;
    }

    public void setSourceCode(AlgorithmOperationSource sourceCode) {
        this.sourceCode = sourceCode;
    }
}
