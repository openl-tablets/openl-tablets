package org.openl.rules.calc.result;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.element.SpreadsheetCell;

public class ScalarResultBuilder implements IResultBuilder {

    private SpreadsheetCell cell;

    private boolean calculateAllCells;
    private IOpenCast openCast;

    public ScalarResultBuilder(SpreadsheetCell spreadsheetCell, IOpenCast openCast, boolean calculateAllCells) {
        this.cell = spreadsheetCell;
        this.calculateAllCells = calculateAllCells;
        this.openCast = openCast;
    }

    public Object makeResult(SpreadsheetResultCalculator result) {
        Object ret;
        if (!calculateAllCells) {
            ret = result.getValue(cell.getRowIndex(), cell.getColumnIndex());
        } else {
            ret = result.getValues()[cell.getRowIndex()][cell.getColumnIndex()];
        }
        return openCast.convert(ret);
    }

}
