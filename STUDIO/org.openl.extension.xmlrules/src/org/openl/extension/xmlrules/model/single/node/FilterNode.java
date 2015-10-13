package org.openl.extension.xmlrules.model.single.node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.ProjectData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType(name = "filter-node")
public class FilterNode extends Node {
    private final Logger log = LoggerFactory.getLogger(FilterNode.class);

    private String fieldName;
    private Comparison comparison;
    private Node conditionValue;
    private Node node;

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
            @XmlElement(name = "condition-filter-node", type = FilterNode.class, required = true)
    })
    public Node getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(Node conditionValue) {
        this.conditionValue = conditionValue;
    }

    @XmlElements({
            @XmlElement(name = "string-node", type=StringNode.class, required = true),
            @XmlElement(name = "number-node", type=NumberNode.class, required = true),
            @XmlElement(name = "boolean-node", type=BooleanNode.class, required = true),
            @XmlElement(name = "range-node", type=RangeNode.class, required = true),
            @XmlElement(name = "expression-node", type=ExpressionNode.class, required = true),
            @XmlElement(name = "function-node", type=FunctionNode.class, required = true),
            @XmlElement(name = "if-node", type=IfNode.class, required = true),
            @XmlElement(name = "filter-node", type=FilterNode.class, required = true)
    })
    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet) {
        node.configure(currentWorkbook, currentSheet);
        if (conditionValue != null) {
            conditionValue.configure(currentWorkbook, currentSheet);
        }
    }

    @Override
    public String toOpenLString() {
        if (isFieldComparisonNode()) {
            String filterString = getFieldComparisonString(false, 0);
            return wrapWithFieldAccess(filterString, true, 0);
        } else if (isFieldNode()) {
            return getFieldString(false, 0);
        } else if (isParentNode()) {
            return getParentString();
        }

        throw new IllegalArgumentException("Unsupported filter node " + toString());
    }

    public String wrapWithFieldAccess(String filterString, boolean lastFieldAccess, int skipFieldsCount) {
        ArrayDeque<FilterNode> nodes = new ArrayDeque<FilterNode>();
        pushToChain(nodes);

        List<String> fieldNames = new ArrayList<String>();

        FilterNode node = nodes.pop();
        FilterNode lastNode = node;
        while (node != null) {
            if (node.isFieldNode()) {
                fieldNames.add(node.getFieldName());
            }
            lastNode = node;
            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        if (lastFieldAccess && lastNode.isFieldComparisonNode()) {
            fieldNames.add(lastNode.getFieldName());
        }

        for (int i = 0; i < fieldNames.size() - skipFieldsCount; i++) {
            filterString = String.format("Field(%s, \"%s\")", filterString, fieldNames.get(i));
        }
        return filterString;
    }

    public String getFieldComparisonString(boolean skipFilters, int skipFieldsCount) {
        if (skipFilters) {
            Node node = getNode();
            if (node instanceof FilterNode) {
                FilterNode filterNode = (FilterNode) node;

                if (filterNode.isFieldComparisonNode()) {
                    return filterNode.getFieldComparisonString(true, skipFieldsCount);
                } else if (filterNode.isFieldNode()) {
                    return filterNode.getFieldString(true, skipFieldsCount);
                } else if (filterNode.isParentNode()) {
                    throw new UnsupportedOperationException("Can't get filter from Parent node");
                } else {
                    log.warn("Unsupported type of node " + filterNode.toString() + ". Skip it");
                }
            } else {
                return node.toOpenLString();
            }
        }

        ArrayDeque<FilterNode> nodes = new ArrayDeque<FilterNode>();
        pushToChain(nodes);

        StringBuilder sb = new StringBuilder();

        FilterNode first = nodes.getFirst();
        sb.append("((Object[]) ").append(first.getNode().toOpenLString()).append(") [");
        sb.append("(o) @ ");

        boolean multipleConditions = false;

        FilterNode node = nodes.pop();
        while (node != null && node.isFieldComparisonNode()) {
            if (multipleConditions) {
                sb.append(" and ");
            } else {
                multipleConditions = true;
            }

            sb.append("Field(o, \"").append(node.getFieldName()).append("\")");
            sb.append(node.getComparison().getValue());
            sb.append(node.getConditionValue().toOpenLString());

            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        int varNumber = 1;
        while (node != null) {
            if (node.isFieldComparisonNode() && node != this) {
                if (multipleConditions) {
                    sb.append(" and ");
                } else {
                    multipleConditions = true;
                }

                sb.append("!isEmpty(");

                String fields = node.getFieldComparisonString(true, 0);
                fields = fields.replace(first.getNode().toOpenLString(), "o"); //FIXME
                sb.append("((Object[]) ").append(fields);

                sb.append(") [");
                sb.append("(o").append(varNumber).append(") @ ");

                sb.append("Field(o").append(varNumber).append(", \"").append(node.getFieldName()).append("\")");
                sb.append(node.getComparison().getValue());
                sb.append(node.getConditionValue().toOpenLString());

                sb.append("]"); // (Object[])

                sb.append(")"); // !isEmpty(

                varNumber++;
            }
            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        sb.append("]");

        return sb.toString();
    }

    public String getFieldString(boolean skipFilters, int skipFieldsCount) {
        Node node = getNode();

        String obj;
        int nextSkipFieldsCount = skipFieldsCount == 0 ? 0 : skipFieldsCount - 1;
        if (node instanceof FilterNode) {
            FilterNode filterNode = (FilterNode) node;
            if (filterNode.isFieldNode()) {
                obj = filterNode.getFieldString(skipFilters, nextSkipFieldsCount);
            } else if (filterNode.isFieldComparisonNode()) {
                obj = filterNode.getFieldComparisonString(skipFilters, nextSkipFieldsCount);
                if (!skipFilters) {
                    obj = filterNode.wrapWithFieldAccess(obj, false, nextSkipFieldsCount);
                }
            } else if (filterNode.isParentNode()) {
                throw new UnsupportedOperationException("Can't get field from Parent node");
            } else {
                log.warn("Unsupported type of node " + node.toOpenLString() + ". Skip it");
                obj = node.toOpenLString();
            }
        } else {
            obj = node.toOpenLString();
        }

        return skipFieldsCount > 0 ? obj : "Field(" + obj + ", \"" + fieldName + "\")";
    }

    protected String getParentString() {
        int parentCount = 1;
        Node node = getNode();
        while (node instanceof FilterNode && ((FilterNode) node).isParentNode()) {
            node = ((FilterNode) node).getNode();
            parentCount++;
        }


        if (node instanceof FilterNode) {
            FilterNode filterNode = (FilterNode) node;
            if (filterNode.isFieldNode()) {
                return filterNode.getFieldString(false, parentCount);
            } else if (filterNode.isFieldComparisonNode()) {
                String filterString = filterNode.getFieldComparisonString(false, 0);
                return filterNode.wrapWithFieldAccess(filterString, true, parentCount);
            }
        }

        throw new IllegalArgumentException("Can't apply Parent() to the node " + (node == null ? "null" : node.toOpenLString()));
    }

    protected boolean isFieldComparisonNode() {
        return comparison != null && conditionValue != null && ProjectData.getCurrentInstance()
                .getFields()
                .contains(fieldName);
    }

    protected boolean isFieldNode() {
        return comparison == null && conditionValue == null && ProjectData.getCurrentInstance()
                .getFields()
                .contains(fieldName);
    }

    protected boolean isParentNode() {
        return comparison == null && conditionValue == null && ProjectData.getCurrentInstance()
                .getTypes()
                .contains(fieldName);
    }

    protected void pushToChain(Deque<FilterNode> nodes) {
        nodes.push(this);
        if (getNode() instanceof FilterNode) {
            ((FilterNode) getNode()).pushToChain(nodes);
        }
    }

    @Override
    public String toString() {
        return "FilterNode{" +
                "fieldName='" + fieldName + '\'' +
                ", comparison=" + comparison +
                ", conditionValue=" + conditionValue +
                "} ";
    }
}
