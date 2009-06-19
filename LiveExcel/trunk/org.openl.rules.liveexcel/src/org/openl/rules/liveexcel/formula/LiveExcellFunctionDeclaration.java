package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;

/**
 * Class for registration new declared UDFs(declared by "OL_DECLARE_FUNCTION").
 * 
 * @author PUdalau
 */
public class LiveExcellFunctionDeclaration implements FreeRefFunction {

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        ParsedDeclaredFunction function = DeclaredFunctionParser.parseFunction(args);        
        setReturnCellType(function, workbook);
        setParamsCellType(function, workbook);
        workbook.getWorkbook().registerUserDefinedFunction(function.getDeclFuncName(), function);
        return new StringEval(function.getDeclFuncName());
    }    
   
    private String getParamSheet(RefEval eval) {
        return eval.toString().substring(eval.toString().indexOf("[")+1,eval.toString().indexOf("!"));
    }
    
    private void setReturnCellType(ParsedDeclaredFunction function, EvaluationWorkbook workbook) {
        Sheet returnSheet = workbook.getWorkbook().getSheet(getParamSheet(function.getReturnCell().getParamCell()));
        function.getReturnCell().setParamType(TypeResolver.resolveType(returnSheet.getRow(function.getReturnCell().
                getParamCell().getRow()).getCell(function.getReturnCell().getParamCell().getColumn()),
                ((LiveExcelWorkbook)workbook.getWorkbook()).getEvaluationContext().getServiceModelAPI()));
                    
    }
    
    private void setParamsCellType(ParsedDeclaredFunction function, EvaluationWorkbook workbook) {
        for (FunctionParam funParam : function.getParameters()) {
            Sheet paramSheet = workbook.getWorkbook().getSheet(getParamSheet(funParam.getParamCell()));
            funParam.setParamType(TypeResolver.resolveType(paramSheet.getRow(funParam.getParamCell().getRow()).
                getCell(funParam.getParamCell().getColumn()),
                ((LiveExcelWorkbook)workbook.getWorkbook()).getEvaluationContext().getServiceModelAPI()));
        }
    }
}
