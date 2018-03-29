package org.openl.excel.parser.dom;

import org.openl.excel.parser.SheetDescriptor;

public class DOMSheetDescriptor implements SheetDescriptor {
    private final String name;

    private int firstRowNum;
    private int firstColNum;

    DOMSheetDescriptor(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
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
