package org.openl.rules.calc;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IDynamicObject;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker for {@link Spreadsheet}.
 *
 * @author DLiauchuk
 */
public class SpreadsheetInvoker extends RulesMethodInvoker<Spreadsheet> {

    private static final Object[][] EMPTY_RESULT = new Object[0][0];
    protected final Object[][] preFetchedResult;

    public SpreadsheetInvoker(Spreadsheet spreadsheet) {
        super(spreadsheet);
        this.preFetchedResult = preFetchResult(spreadsheet);
    }

    @Override
    public boolean canInvoke() {
        return getInvokableMethod().getResultBuilder() != null;
    }

    @Override
    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        var res = new SpreadsheetResultCalculator(getInvokableMethod(),
                (IDynamicObject) target,
                params,
                env,
                preFetchedResult);
        return getInvokableMethod().getResultBuilder().buildResult(res);
    }

    /**
     * Creates a result with constant values that are populated.
     * The cells array is indexed with logical indices (excluding description rows/columns).
     */
    protected Object[][] preFetchResult(Spreadsheet spreadsheet) {
        var cc = spreadsheet.getCells();
        if (cc.length == 0) {
            return EMPTY_RESULT;
        }

        var height = spreadsheet.getHeight();
        var width = spreadsheet.getWidth();

        Object[][] res = new Object[height][width];

        // cells[][] is now indexed with logical indices, so we iterate directly
        for (var i = 0; i < height; i++) {
            var row = cc[i];
            for (var j = 0; j < width; j++) {
                res[i][j] = preFetchValue(row[j]);
            }
        }
        return res;
    }

    private static Object preFetchValue(SpreadsheetCell cell) {
        if (cell == null) {
            return SpreadsheetResultCalculator.DESCRIPTION_CELL;
        }
        return switch (cell.getSpreadsheetCellType()) {
            case EMPTY -> cell.isDefaultPrimitiveCell() ? cell.getValue() : SpreadsheetResultCalculator.EMPTY_CELL;
            case VALUE, CONSTANT -> cell.getValue();
            case METHOD -> SpreadsheetResultCalculator.METHOD_VALUE;
            case DESCRIPTION -> SpreadsheetResultCalculator.DESCRIPTION_CELL;
        };
    }
}
