package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlSeeAlso;

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
    public void configure(String currentWorkbook, String currentSheet) {
        // Do nothing
    }

    public abstract String toOpenLString();
}
