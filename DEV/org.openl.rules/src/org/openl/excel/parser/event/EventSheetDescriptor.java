package org.openl.excel.parser.event;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.excel.parser.SheetDescriptor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class EventSheetDescriptor implements SheetDescriptor {
    private final String name;
    private final int index;
    private final int offset;

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
