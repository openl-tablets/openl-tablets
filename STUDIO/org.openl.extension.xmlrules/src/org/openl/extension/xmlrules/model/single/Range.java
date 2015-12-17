package org.openl.extension.xmlrules.model.single;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class Range {
    private String path;
    private String row;
    private String column;
    private Integer rowCount = 1;
    private Integer colCount = 1;

    public Range() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @XmlElement(required = true)
    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    @XmlElement(required = true)
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
}