package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.single.Cell;

@XmlType(name = "if-error-node")
public class IfErrorNode extends Node {
    public static final String FUNCTION_NAME = "IfError";
    public static final int ARGUMENTS_COUNT = 2;

    private Node value;
    private Node valueIfError;

    public Node getValue() {
        return value;
    }

    public void setValue(Node value) {
        this.value = value;
    }

    public Node getValueIfError() {
        return valueIfError;
    }

    public void setValueIfError(Node valueIfError) {
        this.valueIfError = valueIfError;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        value.configure(currentWorkbook, currentSheet, cell);
        valueIfError.configure(currentWorkbook, currentSheet, cell);
    }

    @Override
    public String toOpenLString() {
        return FUNCTION_NAME + "(" + value.toOpenLString() + ", " + valueIfError.toOpenLString() + ")";
    }
}
