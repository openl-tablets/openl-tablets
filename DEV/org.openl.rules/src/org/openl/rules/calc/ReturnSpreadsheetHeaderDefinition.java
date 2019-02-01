package org.openl.rules.calc;

import java.util.HashSet;
import java.util.Set;

import org.openl.rules.calc.element.SpreadsheetCell;

public class ReturnSpreadsheetHeaderDefinition extends SpreadsheetHeaderDefinition {

    private Set<Integer> returnCells;

    public ReturnSpreadsheetHeaderDefinition(SpreadsheetHeaderDefinition source) {
        super(source.getRow(), source.getColumn());
    }

    public void setReturnCells(SpreadsheetCell[] cells) {
        if (cells.length == 0) {
            returnCells = null;
        } else {
            returnCells = new HashSet<>();
            for (SpreadsheetCell cell : cells) {
                returnCells.add(cell.getColumnIndex());
            }
        }
    }

    public boolean isReturnCell(SpreadsheetCell cell) {
        return getRow() == cell.getRowIndex() && returnCells != null && returnCells.contains(cell.getColumnIndex());
    }

}
