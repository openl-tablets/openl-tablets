package org.openl.rules.calc.result;

import java.util.List;

import org.openl.rules.calc.SpreadsheetResultCalculator;
import org.openl.rules.calc.element.SpreadsheetCell;

public class ScalarResultBuilder implements IResultBuilder {

    private SpreadsheetCell cell;

    private boolean calculateAll;

    public ScalarResultBuilder(List<SpreadsheetCell> notEmpty, boolean calculateAll) {
        cell = notEmpty.get(0);
        this.calculateAll = calculateAll;
    }

    public Object makeResult(SpreadsheetResultCalculator result) {
        if (!calculateAll) {
            return result.getValue(cell.getRowIndex(), cell.getColumnIndex());
        } else {
            return result.getValues()[cell.getRowIndex()][cell.getColumnIndex()];
        }
    }

}
