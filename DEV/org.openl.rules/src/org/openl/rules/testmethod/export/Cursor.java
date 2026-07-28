package org.openl.rules.testmethod.export;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class Cursor {
    private final int rowNum;
    private final int colNum;

    public int getRowNum() {
        return rowNum;
    }

    public int getColNum() {
        return colNum;
    }
}
