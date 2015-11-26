package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
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
    private Integer rowCount = 1;
    private Integer colCount = 1;
    private Boolean hasArrayFormula = Boolean.FALSE;

    public RangeNode() {
    }

    public RangeNode(RangeNode copy) {
        this.currentWorkbook = copy.currentWorkbook;
        this.currentSheet = copy.currentSheet;
        this.path = copy.path;
        this.row = copy.row;
        this.column = copy.column;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        this.currentWorkbook = currentWorkbook;
        this.currentSheet = currentSheet;
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

    @XmlTransient
    public int getRowNumber() {
        return Integer.parseInt(row);
    }

    @XmlTransient
    public int getColumnNumber() {
        return Integer.parseInt(column);
    }

    @XmlElement(defaultValue = "1")
    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    @XmlElement(defaultValue = "1")
    public Integer getColCount() {
        return colCount;
    }

    public void setColCount(Integer colCount) {
        this.colCount = colCount;
    }

    @XmlElement(defaultValue = "false")
    public Boolean getHasArrayFormula() {
        return hasArrayFormula;
    }

    public void setHasArrayFormula(Boolean hasArrayFormula) {
        this.hasArrayFormula = hasArrayFormula;
    }

    @Override
    public String toOpenLString() {
        String cell = CellReference.parse(currentWorkbook, currentSheet, this).getStringValue();
        return String.format("Cell(\"%s\")", cell);
    }
}
