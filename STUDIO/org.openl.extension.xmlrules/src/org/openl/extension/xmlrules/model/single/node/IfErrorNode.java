package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "if-error-node")
public class IfErrorNode extends Node {
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
    public void configure(String currentWorkbook, String currentSheet) {
        value.configure(currentWorkbook, currentSheet);
        valueIfError.configure(currentWorkbook, currentSheet);
    }

    @Override
    public String toOpenLString() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
