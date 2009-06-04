package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationWorkbook;

public class LiveExcellFunctionDeclaration extends LiveExcelFunction {

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        ParsedDeclaredFunction function = DeclaredFunctionParser.parseFunction(args);
        workbook.getWorkbook().registerUserDefinedFunction(function.getDeclFuncName(), function);
        return new StringEval(function.getDeclFuncName());
    }

}
