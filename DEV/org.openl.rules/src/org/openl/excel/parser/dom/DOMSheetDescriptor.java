package org.openl.excel.parser.dom;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.excel.parser.SheetDescriptor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DOMSheetDescriptor implements SheetDescriptor {
    private final String name;
    private final int index;

    private int firstRowNum;
    private int firstColNum;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getFirstRowNum() {
        return firstRowNum;
    }

    public void setFirstRowNum(int firstRowNum) {
        this.firstRowNum = firstRowNum;
    }

    @Override
    public int getFirstColNum() {
        return firstColNum;
    }

    public void setFirstColNum(int firstColNum) {
        this.firstColNum = firstColNum;
    }
}
