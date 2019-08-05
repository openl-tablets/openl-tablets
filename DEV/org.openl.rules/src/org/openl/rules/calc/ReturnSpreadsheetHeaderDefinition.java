package org.openl.rules.calc;

import java.util.HashSet;
import java.util.Set;

import org.openl.rules.calc.element.SpreadsheetCell;

public class ReturnSpreadsheetHeaderDefinition extends SpreadsheetHeaderDefinition {

    private Set<Integer> returnCells;
    private boolean byColumn;

    public ReturnSpreadsheetHeaderDefinition(SpreadsheetHeaderDefinition source) {
        super(source.getDefinition(), source.getRow(), source.getColumn());
    }

    public void setReturnCells(boolean byColumn, SpreadsheetCell... cells) {
        if (cells.length == 0) {
            returnCells = null;
        } else {
            this.returnCells = new HashSet<>();
            this.byColumn = byColumn;
            if (this.byColumn) {
                for (SpreadsheetCell cell : cells) {
                    returnCells.add(cell.getColumnIndex());
                }
            } else {
                for (SpreadsheetCell cell : cells) {
                    returnCells.add(cell.getRowIndex());
                }
            }
        }
    }

    public boolean isReturnCell(SpreadsheetCell cell) {
        if (byColumn) {
            return getRow() == cell.getRowIndex() && returnCells != null && returnCells.contains(cell.getColumnIndex());
        } else {
            return getColumn() == cell.getColumnIndex() && returnCells != null && returnCells
                .contains(cell.getRowIndex());
        }
    }

}
