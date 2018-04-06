package org.openl.excel.parser.sax;

import org.openl.excel.parser.SheetDescriptor;

public final class SAXSheetDescriptor implements SheetDescriptor {
    private final String name;
    private final int index;
    private final String relationId;

    private int firstRowNum;
    private int firstColNum;

    SAXSheetDescriptor(String name, int index, String relationId) {
        this.name = name;
        this.index = index;
        this.relationId = relationId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public String getRelationId() {
        return relationId;
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
