package org.openl.rules.liveexcel.formula;

import org.openl.rules.liveexcel.EvaluationContext;

/**
 * Common LiveExcel function that have access to service model.
 * 
 * @author PUdalau
 */
public abstract class LiveExcelDataAccessFunction extends LiveExcelFunction {
    protected EvaluationContext evaluationContext;

    /**
     * Creates LiveExcelDataAccessFunction with name and context.
     * 
     * @param evaluationContext EvaluationContext that presents service model.
     * @param functionName Name of function.
     */
    public LiveExcelDataAccessFunction(EvaluationContext evaluationContext, String functionName) {
        this.evaluationContext = evaluationContext;
        setDeclFuncName(functionName);
    }
}
