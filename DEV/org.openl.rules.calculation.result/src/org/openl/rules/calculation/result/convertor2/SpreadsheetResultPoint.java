package org.openl.rules.calculation.result.convertor2;

/*-
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2018 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.io.Serializable;

public class SpreadsheetResultPoint implements Serializable {

    private static final long serialVersionUID = 5186952375131099814L;

    private int columnIndex;
    private int rowIndex;

    public SpreadsheetResultPoint(int rowIndex, int columnIndex) {
        this.columnIndex = columnIndex;
        this.rowIndex = rowIndex;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SpreadsheetResultPoint)) {
            return false;
        }
        SpreadsheetResultPoint another = (SpreadsheetResultPoint) obj;
        return another.columnIndex == columnIndex && another.rowIndex == rowIndex;
    }

    @Override
    public int hashCode() {
        return columnIndex + rowIndex * 31;
    }

    @Override
    public String toString() {
        return String.format("column index: %s\nrow index: %s", columnIndex, rowIndex);
    }
}