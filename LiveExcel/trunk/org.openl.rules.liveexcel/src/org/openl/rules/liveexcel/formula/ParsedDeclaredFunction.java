package org.openl.rules.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationWorkbook;

/**
 * ParsedDeclaredFunction - handles parsed ol_declare_function
 * 
 */
public class ParsedDeclaredFunction extends LiveExcelFunction {

    private String declFuncName;
    private FunctionParam returnCell;
    private List<FunctionParam> parameters = new ArrayList<FunctionParam>();

    public String getDeclFuncName() {
        return declFuncName;
    }

    public void setDeclFuncName(String declFuncName) {
        this.declFuncName = declFuncName;
    }

    public String getDescription() {
        return returnCell.getParamName();
    }

    public FunctionParam getReturnCell() {
        return returnCell;
    }

    public void setReturnCell(FunctionParam returnCell) {
        this.returnCell = returnCell;
    }

    public List<FunctionParam> getParameters() {
        return parameters;
    }

    public void setParameters(List<FunctionParam> parameters) {
        this.parameters = parameters;
    }

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        if (args.length != parameters.size()) {
            return ErrorEval.VALUE_INVALID;
        } else {
            for(int i = 0; i < parameters.size(); i++){
                parameters.get(i).getParamCell().setValue((ValueEval)args[i]);
            }
            return returnCell.getParamCell().getInnerValueEval();
        }
    }
}
