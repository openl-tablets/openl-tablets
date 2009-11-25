package org.openl.rules.liveexcel.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.RefEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.OperationEvaluationContext;

/**
 * ParsedDeclaredFunction - handles parsed ol_declare_function
 * 
 */
public class ParsedDeclaredFunction extends LiveExcelFunction {

    private FunctionParam returnCell;
    private List<FunctionParam> parameters = new ArrayList<FunctionParam>();

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

    @Override
    public ValueEval execute(ValueEval[] args, OperationEvaluationContext ec) {
        if (args.length != parameters.size()) {
            return ErrorEval.VALUE_INVALID;
        } else {
            for (int i = 0; i < parameters.size(); i++) {
                RefEval ref = parameters.get(i).getParamCell();
                ec.getWorkbookEvaluator().updateCell(ref.getSheetName(), ref.getRow(), ref.getColumn(),
                        (ValueEval) args[i]);
            }
            EvaluationCell cellToEvaluate = ec.getWorkbook().getSheet(
                    ec.getWorkbook().getSheetIndex(returnCell.getParamCell().getSheetName())).getCell(
                    returnCell.getParamCell().getRow(), returnCell.getParamCell().getColumn());
            return ec.getWorkbookEvaluator().evaluate(cellToEvaluate);
        }
    }
}
