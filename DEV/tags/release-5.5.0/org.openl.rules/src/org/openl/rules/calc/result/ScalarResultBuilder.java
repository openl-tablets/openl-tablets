package org.openl.rules.calc.result;

import java.util.List;

import org.openl.rules.calc.element.SpreadsheetCell;

public class ScalarResultBuilder implements IResultBuilder {

    private SpreadsheetCell cell;

    public ScalarResultBuilder(List<SpreadsheetCell> notEmpty) {
        cell = notEmpty.get(0);
    }

    public Object makeResult(SpreadsheetResult result) {
        return result.getValue(cell.getRowIndex(), cell.getColumnIndex());
    }

}
