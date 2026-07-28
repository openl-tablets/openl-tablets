package org.openl.rules.lang.xls;

import lombok.RequiredArgsConstructor;

import org.openl.rules.table.IGridTable;

@RequiredArgsConstructor
public class TablePart implements Comparable<TablePart> {

    String partName;
    int part;
    boolean vertical;
    int size;

    final IGridTable table;
    final XlsSheetSourceCodeModule source;

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public int getPart() {
        return part;
    }

    public void setPart(int part) {
        this.part = part;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public IGridTable getTable() {
        return table;
    }

    @Override
    public int compareTo(TablePart o) {
        return this.part - o.part;
    }

    public XlsSheetSourceCodeModule getSource() {
        return source;
    }

}
