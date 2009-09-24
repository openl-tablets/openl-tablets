package org.openl.rules.liveexcel;

import java.util.Calendar;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.ss.formula.EvaluationTracker;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.liveexcel.formula.DeclaredFunctionSearcher;
import org.openl.rules.liveexcel.formula.LiveExcelDataAccessFunction;
import org.openl.rules.liveexcel.formula.LiveExcelFunction;
import org.openl.rules.liveexcel.formula.LiveExcelFunctionsPack;

/**
 * Evaluator for any declared LiveExcel function.
 * 
 * @author PUdalau
 */
public class LiveExcelEvaluator {
    private Workbook workbook;
    private EvaluationContext evaluationContext;
    private EvaluationWorkbook evaluationWorkbook;

    /**
     * Parse workbook and create LiveExcelEvaluator.
     * 
     * @param workbook Workbook to parse.
     * @param evaluationContext EvaluationContext associated with evaluator.
     */
    public LiveExcelEvaluator(Workbook workbook, EvaluationContext evaluationContext) {
        this.workbook = workbook;
        this.evaluationContext = evaluationContext;
        evaluationWorkbook = workbook.getCreationHelper().createEvaluationWorkbook();
        new DeclaredFunctionSearcher(workbook).findFunctions();
        registerServiceModelUDFs();
        exposeInOpenL();
    }

    private void registerServiceModelUDFs() {
        for (String functionName : evaluationContext.getServiceModelAPI().getAllServiceModelUDFs()) {
            LiveExcelFunctionsPack.instance().addUDF(workbook, functionName, generateGetterFunction(functionName));
        }
        //stub for root name methods.
        for (String functionName : evaluationContext.getServiceModelAPI().getRootNames()) {
            LiveExcelFunctionsPack.instance().addUDF(workbook, functionName, new FreeRefFunction() {
                public ValueEval evaluate(ValueEval[] args, OperationEvaluationContext ec) {
                    return new NumberEval(0);
                }
            });
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
        UDFFinder functionsPack = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);
        WorkbookEvaluator evaluator = new WorkbookEvaluator(evaluationWorkbook, null, functionsPack);
        evaluationContext.createDataPool(evaluator);
        ValueEval[] processedArgs = new ValueEval[args.length];
        for (int i = 0; i < args.length; i++) {
            processedArgs[i] = createEvalForObject(args[i], evaluator);
        }
        FreeRefFunction udf = evaluator.findUserDefinedFunction(functionName);
        ValueEval result = udf.evaluate(processedArgs, new OperationEvaluationContext(evaluator, evaluationWorkbook,
                -1, -1, -1, new EvaluationTracker()));
        evaluationContext.removeDataPool(evaluator);
        return result;
    }

    private StringEval addObjectToContext(Object object, WorkbookEvaluator evaluator) {
        return new StringEval(evaluationContext.getDataPool(evaluator).add(object));
    }

    private ValueEval createEvalForObject(Object object, WorkbookEvaluator evaluator) {
        if (object instanceof Number) {
            return new NumberEval(((Number) object).doubleValue());
        } else if (object instanceof Boolean) {
            return BoolEval.valueOf((Boolean) object);
        } else if (object instanceof String) {
            return new StringEval((String) object);
        } else if (object instanceof Calendar) {
            return new NumberEval(DateUtil.getExcelDate((Calendar) object, false));
        } else {
            return addObjectToContext(object, evaluator);
        }

    }

    private LiveExcelFunction generateGetterFunction(String name) {
        return new LiveExcelDataAccessFunction(evaluationContext, name) {
            public ValueEval execute(ValueEval[] args, OperationEvaluationContext ec) {
                if (args.length != 1) {
                    return ErrorEval.VALUE_INVALID;
                } else {
                    StringEval objectID = (StringEval) args[0];
                    Object objectToProcess = evaluationContext.getDataPool(ec.getWorkbookEvaluator()).get(
                            objectID.getStringValue());
                    Object result = evaluationContext.getServiceModelAPI().getValue(getDeclFuncName(), objectToProcess);
                    return createEvalForObject(result, ec.getWorkbookEvaluator());
                }
            }
        };
    }
}
