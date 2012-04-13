package org.openl.rules.liveexcel;

import java.util.Calendar;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.forked.ForkedEvaluator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluatorHelper;
import org.apache.poi.ss.usermodel.Row;
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
    
	private static ThreadLocal<Cell> formulaCellLocal = new ThreadLocal<Cell>();  
	
    /**
     * Parse workbook and create LiveExcelEvaluator.
     * 
     * @param workbook Workbook to parse.
     * @param evaluationContext EvaluationContext associated with evaluator.
     */
    public LiveExcelEvaluator(Workbook workbook, EvaluationContext evaluationContext) {
        this.workbook = workbook;
        this.evaluationContext = evaluationContext;
        new DeclaredFunctionSearcher(workbook).findFunctions();
        registerServiceModelUDFs();
        exposeInOpenL();
    }

    private void registerServiceModelUDFs() {
        for (String functionName : evaluationContext.getServiceModelAPI().getAllServiceModelUDFs()) {
            LiveExcelFunctionsPack.instance().addUDF(workbook, functionName, generateGetterFunction(functionName));
        }
        // stub for root name methods.
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
        ForkedEvaluator evaluator = createEvaluator();
        ValueEval[] processedArgs = new ValueEval[args.length];
        for (int i = 0; i < args.length; i++) {
            processedArgs[i] = createEvalForObject(args[i], evaluator.getWorkbookEvaluator());
        }
        Cell formulaCell  = formulaCellLocal.get();
        if(formulaCell == null) {
        	formulaCell = findEmptyCell();
        	formulaCellLocal.set(formulaCell);
        }	
        formulaCell.setCellFormula(createFormula(functionName, processedArgs));
        ValueEval result = evaluator.evaluate(formulaCell.getSheet().getSheetName(), formulaCell.getRowIndex(),
                formulaCell.getColumnIndex());
        finalizeEvaluation(evaluator, formulaCell);
        return result;
    }

    private void finalizeEvaluation(ForkedEvaluator evaluator, Cell formulaCell) {
        removeExecutedCell(formulaCell);
        evaluationContext.removeDataPool(evaluator.getWorkbookEvaluator());
    }

    private ForkedEvaluator createEvaluator() {
        UDFFinder functionsPack = LiveExcelFunctionsPack.instance().getUDFFinderLE(workbook);
        ForkedEvaluator evaluator = ForkedEvaluator.create(workbook, null, functionsPack);
        evaluationContext.createDataPool(evaluator.getWorkbookEvaluator());
        return evaluator;
    }
    
    /**
     * @return Cell used for execution of new formula
     */
    private synchronized Cell findEmptyCell() {
        int lastRowIndex = workbook.getSheetAt(0).getLastRowNum();
        Row row = workbook.getSheetAt(0).createRow(lastRowIndex + 1);
        return row.createCell(0);
    }

    private void removeExecutedCell(Cell cell) {
    	// We now need to remove formulaCell, as it is common for Thread
//        workbook.getSheetAt(0).removeRow(cell.getRow());
    }

    /**
     * Generates string representation of formula for cell by function name and arguments.
     */
    private String createFormula(String functionName, ValueEval[] args) {
        StringBuffer formula = new StringBuffer(functionName + "(");
        for (int i = 0; i < args.length; i++) {
            formula.append(extractStringValueFromValueEval(args[i]));
            if (i < args.length - 1) {
                formula.append(',');
            }
        }
        formula.append(')');
        return formula.toString();
    }

    private String extractStringValueFromValueEval(ValueEval eval) {
        return FormulaEvaluatorHelper.eval2Cell(eval).formatAsString();
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
