package org.openl.rules.liveexcel.formula.lookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.OperationEvaluationContext;
import org.openl.rules.liveexcel.formula.ParsedDeclaredFunction;

/**
 * Evaluator for lookup. Data for lookup must be represented as linearized(In
 * last column - return value, all columns except last - input parameters
 * according to return value)
 * 
 * @author PUdalau
 */
public class LiveExcelLookup extends ParsedDeclaredFunction {

    private static final Log log = LogFactory.getLog(LiveExcelLookup.class);

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

    @Override
    public ValueEval execute(ValueEval[] args, OperationEvaluationContext ec) {
        int paramsCount = this.getParameters().size();
        if (args.length != paramsCount) {
            return ErrorEval.VALUE_INVALID;
        } else {
            for (int i = 0; i < lookupData.getHeight(); i++) {
                boolean matched = true;
                for (int j = 0; j < paramsCount; j++) {
                    if (!isLookupParamMatched(i, paramsCount - 1 - j, (ValueEval) args[j])) {
                        matched = false;
                        break;
                    }
                }
                if (matched) {
                    return lookupData.getValue(lookupData.getWidth() - 1, i);
                }
            }
            log.warn("No matched values in lookup [" +declFuncName + "]");
            return ErrorEval.NA;
        }
    }

    private boolean isLookupParamMatched(int lookupRowIndex, int paramIndex, ValueEval expectedValue) {
        lookupRowIndex = findParamValue(lookupRowIndex, paramIndex);
        ValueEval param = lookupData.getValue(paramIndex, lookupRowIndex);
        return LookupComparer.isMatched(param, expectedValue);
    }

    private int findParamValue(int lookupRowIndex, int paramIndex) {
        // looks for first non-empty previous lookup param value(if param
        // skipped it will be taken from previous row)
        while (lookupRowIndex > 0 && lookupData.isBlank(paramIndex, lookupRowIndex)) {
            lookupRowIndex--;
        }
        return lookupRowIndex;
    }
}
