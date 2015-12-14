package org.openl.extension.xmlrules.model.single.node;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.model.single.node.expression.Operator;
import org.openl.extension.xmlrules.model.single.node.expression.RangeExpressionResolver;

@XmlType(name = "named-node")
public class NamedRangeNode extends Node {
    private String name;

    @XmlElement(name = "value", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlTransient
    public RangeNode getRangeNode() {
        return ProjectData.getCurrentInstance().getNamedRanges().get(name);
    }

    @Override
    public String toOpenLString() {
        RangeNode rangeNode = getRangeNode();
        RangeExpressionResolver expressionResolver = new RangeExpressionResolver();

        RangeNode r1 = new RangeNode(rangeNode);
        r1.setRowCount(1);
        r1.setColCount(1);

        RangeNode r2 = new RangeNode(rangeNode);
        r2.setRow(String.valueOf(rangeNode.getRowNumber() + rangeNode.getRowCount() - 1));
        r2.setColumn(String.valueOf(rangeNode.getColumnNumber() + rangeNode.getColCount() - 1));
        r1.setRowCount(1);
        r1.setColCount(1);

        ExpressionContext context = ExpressionContext.getInstance();
        boolean canHandleArrayOperators = context.isCanHandleArrayOperators();
        try {
            context.setCanHandleArrayOperators(true);
            return expressionResolver.resolve(r1, r2, Operator.Range);
        } finally {
            context.setCanHandleArrayOperators(canHandleArrayOperators);
        }
    }
}
