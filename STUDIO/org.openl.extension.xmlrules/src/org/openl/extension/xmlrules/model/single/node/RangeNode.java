package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.utils.CellReference;

@XmlType(name = "range-node")
public class RangeNode extends Node {
    private String currentWorkbook;
    private String currentSheet;
    private String range;

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        this.currentWorkbook = currentWorkbook;
        this.currentSheet = currentSheet;
    }

    @XmlElement(required = true)
    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    @Override
    public String toOpenLString() {
        String cell = CellReference.parse(currentWorkbook, currentSheet, range).getStringValue();
        // FIXME Remove this cast to String
        return String.format("(String) Cell(\"%s\")", cell);
    }
}
