package org.openl.rules.calc;

import java.util.List;

public class ScalarResultBuilder implements IResultBuilder {

    SCell cell;

    public ScalarResultBuilder(List<SCell> notEmpty) {
        cell = notEmpty.get(0);
    }

    public Object makeResult(SpreadsheetResult res) {

        return res.getValue(cell.getRow(), cell.getColumn());
    }

}
