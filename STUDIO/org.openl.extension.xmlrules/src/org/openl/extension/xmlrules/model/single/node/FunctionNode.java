package org.openl.extension.xmlrules.model.single.node;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.model.single.Cell;

@XmlType(name = "function-node")
public class FunctionNode extends Node {
    private String name;
    private List<Node> arguments = new ArrayList<Node>();

    @XmlElement(required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public void setArguments(List<Node> arguments) {
        this.arguments = arguments;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        for (Node argument : arguments) {
            argument.configure(currentWorkbook, currentSheet, cell);
        }
    }

    @Override
    public String toOpenLString() {
        StringBuilder builder = new StringBuilder(name);
        builder.append('(');
        for (int i = 0; i < arguments.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(arguments.get(i).toOpenLString());
        }
        builder.append(')');
        return builder.toString();
    }
}
