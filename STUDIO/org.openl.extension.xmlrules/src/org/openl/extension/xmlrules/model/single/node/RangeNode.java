package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.utils.CellReference;

@XmlType(name = "range-node")
public class RangeNode extends Node {
    private String currentWorkbook;
    private String currentSheet;
    private String path;
    private String row;
    private String column;

    private boolean relative = true; // Currently node address is relative

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        this.currentWorkbook = currentWorkbook;
        this.currentSheet = currentSheet;

        if (relative) {
            RangeNode cellAddress = cell.getAddress();
            column = String.valueOf(Integer.parseInt(cellAddress.getColumn()) + Integer.parseInt(column));
            row = String.valueOf(Integer.parseInt(cellAddress.getRow()) + Integer.parseInt(row));
            relative = false;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    @Override
    public String toOpenLString() {
        String cell = CellReference.parse(currentWorkbook, currentSheet, this).getStringValue();
        return String.format("Cell(\"%s\")", cell);
    }
}
