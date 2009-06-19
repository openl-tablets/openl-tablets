package org.openl.rules.liveexcel;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.LiveExcelDataAccessFunction;
import org.openl.rules.liveexcel.formula.LiveExcelFunction;

/**
 * Evaluator for any declared LiveExcel function.
 * 
 * @author PUdalau
 */
public class LiveExcelEvaluator {
    private Workbook workbook;
    private EvaluationContext evaluationContext;

    /**
     * Parse workbook and create LiveExcelEvaluator.
     * 
     * @param workbook Workbook to parse.
     * @param evaluationContext EvaluationContext associated with evaluator.
     */
    public LiveExcelEvaluator(Workbook workbook, EvaluationContext evaluationContext) {
        this.workbook = workbook;
        this.evaluationContext = evaluationContext;
        registerServiceModelUDFs();
        new DeclaredFunctionSearcher(workbook).findFunctions();        
        exposeInOpenL();
    }

    private void registerServiceModelUDFs() {
        for (String functionName : evaluationContext.getServiceModelAPI().getAllServiceModelUDFs()) {
            workbook.registerUserDefinedFunction(functionName, generateGetterFunction(functionName));
        }
    }

    private void exposeInOpenL() {
        // getServiceModelObjectDomainType( function name in input cell formula)
    }

    /**
     * Evaluates any declared function with specified arguments
     * 
     * @param functionName Name of function.
     * @param args Arguments for function.
     * @return Result of evaluation.
     */
    public ValueEval evaluateServiceModelUDF(String functionName, Object[] args) {
        evaluationContext.resetContext();
        ValueEval[] processedArgs = new ValueEval[args.length];
        for (int i = 0; i < args.length; i++) {
            processedArgs[i] = createEvalForObject(args[i]);
        }
        return workbook.getUserDefinedFunction(functionName).evaluate(processedArgs, null, 0, 0, 0);
    }

    private StringEval addObjectToContext(Object object) {
        return new StringEval(evaluationContext.getDataPool().add(object));
    }

    private ValueEval createEvalForObject(Object object) {
        if (object instanceof Number) {
            return new NumberEval(((Number) object).doubleValue());
        } else if (object instanceof Boolean) {
            return BoolEval.valueOf((Boolean) object);
        } else if (object instanceof String) {
            return new StringEval((String) object);
        } else {
            return addObjectToContext(object);
        }

    }

    private LiveExcelFunction generateGetterFunction(String name) {
        return new LiveExcelDataAccessFunction(evaluationContext, name) {
            public ValueEval execute(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow,
                    int srcCellCol) {
                if (args.length != 1) {
                    return ErrorEval.VALUE_INVALID;
                } else {
                    StringEval objectID = (StringEval) args[0];
                    Object objectToProcess = evaluationContext.getDataPool().get(objectID.getStringValue());
                    Object result = evaluationContext.getServiceModelAPI().getValue(getDeclFuncName(), objectToProcess);
                    return createEvalForObject(result);
                }
            }
        };
    }
}
