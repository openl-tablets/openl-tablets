package org.openl.extension.xmlrules.model.single.node;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

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

    @XmlElements({
            @XmlElement(name = "string-node", type=StringNode.class),
            @XmlElement(name = "number-node", type=NumberNode.class),
            @XmlElement(name = "boolean-node", type=BooleanNode.class),
            @XmlElement(name = "range-node", type=RangeNode.class),
            @XmlElement(name = "expression-node", type=ExpressionNode.class),
            @XmlElement(name = "function-node", type=FunctionNode.class),
            @XmlElement(name = "if-node", type=IfNode.class),
            @XmlElement(name = "field-node", type=FieldNode.class, required = true),
            @XmlElement(name = "filter-node", type=FilterNode.class, required = true),
            @XmlElement(name = "parent-node", type=ParentNode.class, required = true)
    })
    public List<Node> getArguments() {
        return arguments;
    }

    public void setArguments(List<Node> arguments) {
        this.arguments = arguments;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        for (Node argument : arguments) {
            argument.configure(currentWorkbook, currentSheet);
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
