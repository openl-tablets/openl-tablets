package org.openl.excel.parser.event;

import org.openl.excel.parser.SheetDescriptor;

public class EventSheetDescriptor implements SheetDescriptor {
    private final String name;
    private final int offset;

    private int firstRowNum;
    private int firstColNum;

    EventSheetDescriptor(String name, int offset) {
        this.name = name;
        this.offset = offset;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getOffset() {
        return offset;
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
