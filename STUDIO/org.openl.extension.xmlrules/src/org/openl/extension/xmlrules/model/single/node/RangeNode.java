package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.XmlRulesPath;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.Range;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.utils.CellReference;

@XmlType(name = "range-node")
public class RangeNode extends Node {
    private Range range = new Range();
    private String currentWorkbook;
    private String currentSheet;
    private Boolean hasArrayFormula = Boolean.FALSE;

    public RangeNode() {
    }

    public RangeNode(RangeNode copy) {
        this.currentWorkbook = copy.currentWorkbook;
        this.currentSheet = copy.currentSheet;
        this.range = new Range();
        this.range.setPath(copy.range.getPath());
        this.range.setRow(copy.range.getRow());
        this.range.setColumn(copy.range.getColumn());
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        this.currentWorkbook = currentWorkbook;
        this.currentSheet = currentSheet;
    }

//    @XmlElement(required = true) // TODO Uncomment when LE part will be implemented
    @XmlTransient // TODO Remove when LE part will be implemented
    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

//    @XmlTransient // TODO Uncomment when LE part will be implemented
    public String getPath() {
        return range.getPath();
    }

    public void setPath(String path) {
        range.setPath(path);
    }

//    @XmlTransient // TODO Uncomment when LE part will be implemented
    public String getRow() {
        return range.getRow();
    }

    public void setRow(String row) {
        range.setRow(row);
    }

//    @XmlTransient // TODO Uncomment when LE part will be implemented
    public String getColumn() {
        return range.getColumn();
    }

    public void setColumn(String column) {
        range.setColumn(column);
    }

    @XmlTransient
    public int getRowNumber() {
        return range.getRowNumber();
    }

    @XmlTransient
    public int getColumnNumber() {
        return range.getColumnNumber();
    }

    @XmlElement(defaultValue = "1")
//    @XmlTransient // TODO Uncomment when LE part will be implemented
    public Integer getRowCount() {
        return range.getRowCount();
    }

    public void setRowCount(Integer rowCount) {
        range.setRowCount(rowCount);
    }

    @XmlElement(defaultValue = "1")
//    @XmlTransient // TODO Uncomment when LE part will be implemented
    public Integer getColCount() {
        return range.getColCount();
    }

    public void setColCount(Integer colCount) {
        range.setColCount(colCount);
    }

    @XmlElement(defaultValue = "false")
    public Boolean getHasArrayFormula() {
        return hasArrayFormula;
    }

    public void setHasArrayFormula(Boolean hasArrayFormula) {
        this.hasArrayFormula = hasArrayFormula;
    }

    @XmlTransient
    public String getAddress() {
        return getReference().getStringValue();
    }

    @XmlTransient
    public CellReference getReference() {
        return CellReference.parse(currentWorkbook, currentSheet, this);
    }

    @Override
    public String toOpenLString() {
        CellReference reference = getReference();

        XmlRulesPath path = ExpressionContext.getInstance().getCurrentPath();
        if (!reference.getWorkbook().equals(path.getWorkbook())) {
            return String.format("Cell(\"%s\", \"%s\", %d, %d)",
                    reference.getWorkbook(),
                    reference.getSheet(),
                    reference.getRowNumber(),
                    reference.getColumnNumber());
        } else if (!reference.getSheet().equals(path.getSheet())) {
            return String.format("Cell(\"%s\", %d, %d)",
                    reference.getSheet(),
                    reference.getRowNumber(),
                    reference.getColumnNumber());
        } else {
            return String.format("Cell(%d, %d)", reference.getRowNumber(), reference.getColumnNumber());
        }
    }
}
