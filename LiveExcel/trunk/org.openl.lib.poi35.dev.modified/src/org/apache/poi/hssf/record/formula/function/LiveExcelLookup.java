package org.apache.poi.hssf.record.formula.function;

import java.util.List;

import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.formula.EvaluationWorkbook;

public class LiveExcelLookup extends LiveExcelFunction {
    public LiveExcelLookup(String name, HSSFCell outputCell, List<HSSFCell> inputCells) {
        super(name, outputCell, inputCells);
    }
    
    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        return super.evaluate(args, workbook, srcCellSheet, srcCellRow, srcCellCol);
    }
}
