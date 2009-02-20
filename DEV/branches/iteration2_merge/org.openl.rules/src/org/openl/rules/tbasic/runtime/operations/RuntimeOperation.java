package org.openl.rules.tbasic.runtime.operations;

import org.openl.rules.tbasic.compile.AlgorithmOperationSource;
import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.debug.TBasicOperationTraceObject;
import org.openl.vm.Tracer;

/**
 * 
 */
public abstract class RuntimeOperation {
    private AlgorithmOperationSource sourceCode;

    public Result execute(TBasicContextHolderEnv environment, Object param, boolean debugMode) {
        Result result = null;

        TBasicOperationTraceObject operationTracer = null;

        if (debugMode) {
            operationTracer = new TBasicOperationTraceObject(this);
            operationTracer.setFieldValues(environment.getTbasicTarget().getFieldValues());
            Tracer.getTracer().push(operationTracer);
        }

        try {

            result = execute(environment, param);

        } finally {
            if (debugMode) {
                operationTracer.setResult(result);
                Tracer.getTracer().pop();
            }
        }

        return result;
    }

    public abstract Result execute(TBasicContextHolderEnv environment, Object param);

    public void setSourceCode(AlgorithmOperationSource sourceCode) {
        this.sourceCode = sourceCode;
    }

    /**
     * @return the source code
     */
    public AlgorithmOperationSource getSourceCode() {
        return sourceCode;
    }
}
