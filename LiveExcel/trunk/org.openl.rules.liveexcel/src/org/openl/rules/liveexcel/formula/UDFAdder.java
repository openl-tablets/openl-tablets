package org.openl.rules.liveexcel.formula;

import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.toolpack.MainToolPacksHandler;
import org.apache.poi.hssf.usermodel.HSSFName;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.EvaluationWorkbook;

public class UDFAdder {
    
    private String functionName;
    HSSFWorkbook workbook;
    
    public UDFAdder(String functionName, HSSFWorkbook workbook) {        
        this.functionName = functionName;
        this.workbook = workbook;
    }

    private void addNewUDF() {
        HSSFName declarationOfUDF = workbook.getName(functionName);
        if (declarationOfUDF == null) {
            declarationOfUDF = workbook.createName();
        }
        declarationOfUDF.setNameName(functionName);
        declarationOfUDF.setFunction(true);
    }

    
    public void makeUDF() {
        addNewUDF();
        LiveExcelFunctionsPack pack = new LiveExcelFunctionsPack();
        pack.addFunction(functionName, new LiveExcelFunction(null, null, null) {
            public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow,
                    int srcCellCol) {
                if (args.length != 1) {
                    return ErrorEval.VALUE_INVALID;
                } else {
                    try {
                        return new NumberEval(((NumberEval) args[0]).getNumberValue() + 1);
                    } catch (Exception e) {
                        return ErrorEval.VALUE_INVALID;
                    }
                }
            }
        });
        MainToolPacksHandler.instance().addToolPack(pack);
    }

}
