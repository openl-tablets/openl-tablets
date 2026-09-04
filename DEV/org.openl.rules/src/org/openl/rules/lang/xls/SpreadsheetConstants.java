package org.openl.rules.lang.xls;

import lombok.Getter;
import org.apache.poi.ss.SpreadsheetVersion;

public class SpreadsheetConstants {

    @Getter
    private final int maxRowIndex;
    @Getter
    private final int maxColumnIndex;

    public SpreadsheetConstants(SpreadsheetVersion spreadsheetVersion) {
        this.maxRowIndex = spreadsheetVersion.getLastRowIndex();
        this.maxColumnIndex = spreadsheetVersion.getLastColumnIndex();
    }
}
