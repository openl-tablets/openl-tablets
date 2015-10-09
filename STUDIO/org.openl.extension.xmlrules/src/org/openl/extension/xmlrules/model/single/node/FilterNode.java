package org.openl.extension.xmlrules.model.single.node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "filter-node")
public class FilterNode extends ChainedNode {
    private String fieldName;
    private Comparison comparison;
    private Node conditionValue;

    @XmlElement(name = "field-name", required = true)
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Comparison getComparison() {
        return comparison;
    }

    public void setComparison(Comparison comparison) {
        this.comparison = comparison;
    }

    @XmlElements({
            @XmlElement(name = "condition-string-node", type = StringNode.class, required = true),
            @XmlElement(name = "condition-number-node", type = NumberNode.class, required = true),
            @XmlElement(name = "condition-boolean-node", type = BooleanNode.class, required = true),
            @XmlElement(name = "condition-range-node", type = RangeNode.class, required = true),
            @XmlElement(name = "condition-expression-node", type = ExpressionNode.class, required = true),
            @XmlElement(name = "condition-function-node", type = FunctionNode.class, required = true),
            @XmlElement(name = "condition-if-node", type = IfNode.class, required = true),
            @XmlElement(name = "condition-field-node", type = FieldNode.class, required = true),
            @XmlElement(name = "condition-filter-node", type = FilterNode.class, required = true),
            @XmlElement(name = "condition-parent-node", type=ParentNode.class, required = true)
    })
    public Node getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(Node conditionValue) {
        this.conditionValue = conditionValue;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        super.configure(currentWorkbook, currentSheet);
        conditionValue.configure(currentWorkbook, currentSheet);
    }

    @Override
    public String toOpenLString() {
        String filterString = toOpenLString(false, 0);
        return wrapWithFieldAccess(filterString, true, 0);
    }

    public String wrapWithFieldAccess(String filterString, boolean lastFieldAccess, int skipFieldsCount) {
        ArrayDeque<ChainedNode> nodes = new ArrayDeque<ChainedNode>();
        pushToChain(nodes);

        List<String> fieldNames = new ArrayList<String>();

        ChainedNode node = nodes.pop();
        ChainedNode lastNode = node;
        while (node != null) {
            if (node instanceof FieldNode) {
//                filterString = String.format("Field(%s, \"%s\")", filterString, ((FieldNode) node).getFieldName());
                fieldNames.add(((FieldNode) node).getFieldName());
            }
            lastNode = node;
            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        if (lastFieldAccess && lastNode instanceof FilterNode) {
//            filterString = String.format("Field(%s, \"%s\")", filterString, ((FilterNode) lastNode).getFieldName());
            fieldNames.add(((FilterNode) lastNode).getFieldName());
        }

        for (int i = 0; i < fieldNames.size() - skipFieldsCount; i++) {
            filterString = String.format("Field(%s, \"%s\")", filterString, fieldNames.get(i));
        }
        return filterString;
    }

    public String toOpenLString(boolean skipFilters, int skipFieldsCount) {
        if (skipFilters) {
            Node node = getNode();
            if (node instanceof FilterNode) {
                return ((FilterNode) node).toOpenLString(true, skipFieldsCount);
            } else if (node instanceof FieldNode) {
                return ((FieldNode) node).toOpenLString(true, skipFieldsCount);
            } else if (node instanceof ParentNode) {
                throw new UnsupportedOperationException("Can't get filter from Parent node");
            } else {
                return node.toOpenLString();
            }
        }

        ArrayDeque<ChainedNode> nodes = new ArrayDeque<ChainedNode>();
        pushToChain(nodes);

        StringBuilder sb = new StringBuilder();

        ChainedNode first = nodes.getFirst();
        sb.append("((Object[]) ").append(first.getNode().toOpenLString()).append(") [");
        sb.append("(o) @ ");

        boolean multipleConditions = false;

        ChainedNode node = nodes.pop();
        while (node instanceof FilterNode) {
            if (multipleConditions) {
                sb.append(" and ");
            } else {
                multipleConditions = true;
            }

            FilterNode filterNode = (FilterNode) node;
            sb.append("Field(o, \"").append(filterNode.getFieldName()).append("\")");
            sb.append(filterNode.getComparison().getValue());
            sb.append(filterNode.getConditionValue().toOpenLString());

            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        int varNumber = 1;
        while (node != null) {
            if (node instanceof FilterNode) {
                FilterNode filterNode = (FilterNode) node;

                if (multipleConditions) {
                    sb.append(" and ");
                } else {
                    multipleConditions = true;
                }

                sb.append("!isEmpty(");

                String fields = filterNode.toOpenLString(true, 0);
                fields = fields.replace(first.getNode().toOpenLString(), "o"); //FIXME
                sb.append("((Object[]) ").append(fields);

                sb.append(") [");
                sb.append("(o").append(varNumber).append(") @ ");

                sb.append("Field(o").append(varNumber).append(", \"").append(filterNode.getFieldName()).append("\")");
                sb.append(filterNode.getComparison().getValue());
                sb.append(filterNode.getConditionValue().toOpenLString());

                sb.append("]"); // (Object[])

                sb.append(")"); // !isEmpty(

                varNumber++;
            }
            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        sb.append("]");

        return sb.toString();
    }
}
