package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlSeeAlso;

import org.openl.extension.xmlrules.model.single.Cell;

@XmlSeeAlso({
        StringNode.class,
        NumberNode.class,
        BooleanNode.class,

        RangeNode.class,
        ExpressionNode.class,
        FunctionNode.class,
        IfNode.class,
        IfErrorNode.class,
        FilterNode.class,

        FailureNode.class
})
public abstract class Node {
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        // Do nothing
    }

    public abstract String toOpenLString();
}
