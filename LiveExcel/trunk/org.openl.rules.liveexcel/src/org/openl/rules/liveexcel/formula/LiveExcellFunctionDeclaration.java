package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.EvaluationWorkbook;

/**
 * Class for registration new declared UDFs(declared by "OL_DECLARE_FUNCTION").
 * 
 * @author PUdalau
 */
public class LiveExcellFunctionDeclaration implements FreeRefFunction {

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        ParsedDeclaredFunction function = DeclaredFunctionParser.parseFunction(args);
        workbook.getWorkbook().registerUserDefinedFunction(function.getDeclFuncName(), function);
        return new StringEval(function.getDeclFuncName());
    }

}
