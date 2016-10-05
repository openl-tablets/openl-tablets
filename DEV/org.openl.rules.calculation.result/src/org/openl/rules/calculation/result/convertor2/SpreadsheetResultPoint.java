package org.openl.rules.calculation.result.convertor2;

/*
 * #%L
 * OpenL - DEV - Rules - Calculation Result
 * %%
 * Copyright (C) 2015 - 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
        EqualsBuilder builder = new EqualsBuilder();
        if (!(obj instanceof SpreadsheetResultPoint)) {
            return false;
        }
        SpreadsheetResultPoint another = (SpreadsheetResultPoint) obj;
        builder.append(another.columnIndex, columnIndex);
        builder.append(another.rowIndex, rowIndex);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        int hashCode = new HashCodeBuilder().append(columnIndex).append(rowIndex).toHashCode();

        return hashCode;
    }

    @Override
    public String toString() {
        return String.format("column index: %s\nrow index: %s", columnIndex, rowIndex);
    }
}