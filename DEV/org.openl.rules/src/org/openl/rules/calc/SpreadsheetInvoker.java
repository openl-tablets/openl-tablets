package org.openl.rules.calc;

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

        var height = spreadsheet.getHeight();
        var width = spreadsheet.getWidth();

        Object[][] res = cc.length == 0 ? EMPTY_RESULT : new Object[height][width];

        // cells[][] is now indexed with logical indices, so we iterate directly
        for (var i = 0; i < height; i++) {
            var row = cc[i];
            for (var j = 0; j < width; j++) {
                var cell = row[j];
                if (cell != null) {
                    switch (cell.getSpreadsheetCellType()) {
                        case EMPTY:
                            res[i][j] = cell.isDefaultPrimitiveCell() ? cell.getValue()
                                    : SpreadsheetResultCalculator.EMPTY_CELL;
                            break;
                        case VALUE:
                        case CONSTANT:
                            res[i][j] = cell.getValue();
                            break;
                        case METHOD:
                            res[i][j] = SpreadsheetResultCalculator.METHOD_VALUE;
                            break;
                        case DESCRIPTION:
                            res[i][j] = SpreadsheetResultCalculator.DESCRIPTION_CELL;
                            break;
                    }
                } else {
                    res[i][j] = SpreadsheetResultCalculator.DESCRIPTION_CELL;
                }
            }
        }
        return res;
    }
}
