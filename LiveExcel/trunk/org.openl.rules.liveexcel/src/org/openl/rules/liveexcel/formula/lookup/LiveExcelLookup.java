package org.openl.rules.liveexcel.formula.lookup;

import org.apache.poi.hssf.record.formula.eval.BlankEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.Eval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.StringValueEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.openl.rules.liveexcel.formula.LiveExcelFunction;

/**
 * Evaluator for lookup. Data for lookup must be represented as linearized(In
 * last column - return value, all columns except last - input parameters
 * according to return value)
 * 
 * @author PUdalau
 */
public class LiveExcelLookup extends LiveExcelFunction {
    private Grid lookupData;

    /**
     * Creates LiveExcelLookup with associated data for lookup.
     * 
     * @param lookupData {@link Grid} for lookup. It must be linearized.
     */
    public LiveExcelLookup(Grid lookupData) {
        this.lookupData = lookupData;
    }

    /**
     * @return Data for lookup.
     */
    public Grid getLookupData() {
        return lookupData;
    }

    public ValueEval evaluate(Eval[] args, EvaluationWorkbook workbook, int srcCellSheet, int srcCellRow, int srcCellCol) {
        if (args.length != lookupData.getWidth() - 1) {
            return ErrorEval.VALUE_INVALID;
        } else {
            for (int i = 0; i < lookupData.getHeight(); i++) {
                boolean matched = true;
                for (int j = 0; j < lookupData.getWidth() - 1; j++) {
                    if (!lookupData.getValue(j, i).equals(((StringValueEval) args[j]).getStringValue())) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    return new StringEval(lookupData.getValue(lookupData.getWidth() - 1, i));
                }
            }
            return BlankEval.INSTANCE;
        }
    }
}
