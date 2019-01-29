package org.openl.rules.lang.xls;

import org.apache.poi.ss.SpreadsheetVersion;

public class SpreadsheetConstants {

    private final int maxRowIndex;
    private final int maxColumnIndex;

    public SpreadsheetConstants(SpreadsheetVersion spreadsheetVersion) {
        this.maxRowIndex = spreadsheetVersion.getLastRowIndex();
        this.maxColumnIndex = spreadsheetVersion.getLastColumnIndex();
    }

    public int getMaxRowIndex() {
        return maxRowIndex;
    }

    public int getMaxColumnIndex() {
        return maxColumnIndex;
    }
}
