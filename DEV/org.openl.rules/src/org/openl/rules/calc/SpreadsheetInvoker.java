package org.openl.rules.calc;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.trace.SpreadsheetTraceObject;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invoker for {@link Spreadsheet}.
 *
 * @author DLiauchuk
 */
public class SpreadsheetInvoker extends RulesMethodInvoker<Spreadsheet> {

    private final Logger log = LoggerFactory.getLogger(SpreadsheetInvoker.class);
    protected Object[][] preFetchedResult;

    public SpreadsheetInvoker(Spreadsheet spreadsheet) {
        super(spreadsheet);
        this.preFetchedResult = preFetchResult(spreadsheet);
    }

    public boolean canInvoke() {
        return getInvokableMethod().getResultBuilder() != null;
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(
                getInvokableMethod(), (IDynamicObject) target, params, env,
                preFetchedResult);
        return getInvokableMethod().getResultBuilder().makeResult(res);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {

        Object result = null;

        SpreadsheetTraceObject traceObject = (SpreadsheetTraceObject) getTraceObject(params);
        Tracer.begin(traceObject);

        try {
            SpreadsheetResultCalculator res = new SpreadsheetResultCalculator(
                    getInvokableMethod(), (IDynamicObject) target, params, env,
                    traceObject);

            result = getInvokableMethod().getResultBuilder().makeResult(res);
            traceObject.setResult(result);
        } catch (RuntimeException e) {
            traceObject.setError(e);
            throw e;
        } finally {
            Tracer.end();
        }
        return result;
    }

    /**
     * Creates a result with constant values that are populated
     *
     * @param spreadsheet
     */
    protected Object[][] preFetchResult(Spreadsheet spreadsheet) {
        SpreadsheetCell[][] cc = spreadsheet.getCells();
        Object[][] res = new Object[cc.length][cc[0].length];

        for (int i = 0; i < cc.length; i++) {
            SpreadsheetCell[] row = cc[i];
            for (int j = 0; j < row.length; j++) {
                SpreadsheetCell cell = row[j];
                switch (cell.getKind()) {
                    case EMPTY:
                        res[i][j] = null;
                        break;
                    case VALUE:
                        res[i][j] = cell.getValue();
                        break;
                    case METHOD:
                        res[i][j] = SpreadsheetResultCalculator.NEED_TO_CALCULATE_VALUE;
                        break;

                }

            }

        }

        return res;

    }
}
