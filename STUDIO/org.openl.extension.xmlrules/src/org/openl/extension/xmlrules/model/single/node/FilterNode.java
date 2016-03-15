package org.openl.extension.xmlrules.model.single.node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
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

    @XmlElement(name = "condition")
    public Node getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(Node conditionValue) {
        this.conditionValue = conditionValue;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void configure(String currentWorkbook, String currentSheet, Cell cell) {
        if (node != null) {
            node.configure(currentWorkbook, currentSheet, cell);
        }
        if (conditionValue != null) {
            conditionValue.configure(currentWorkbook, currentSheet, cell);
        }
    }

    @Override
    public String toOpenLString() {
        ExpressionContext instance = ExpressionContext.getInstance();
        boolean canHandleArrayOperators = instance.isCanHandleArrayOperators();
        try {
            instance.setCanHandleArrayOperators(false);
            if (isFieldComparisonNode()) {
                String filterString = getFieldComparisonString(false, 0);
                return wrapWithFieldAccess(filterString, true, 0);
            } else if (isFieldNode()) {
                return getFieldString(false, 0);
            } else if (isParentNode()) {
                return getParentString();
            } else if (isDataInstanceNode()) {
                return getDataInstanceString();
            }

            throw new IllegalArgumentException("Unsupported filter node " + toString());
        } finally {
            instance.setCanHandleArrayOperators(canHandleArrayOperators);
        }
    }

    public String wrapWithFieldAccess(String filterString, boolean lastFieldAccess, int skipFieldsCount) {
        ArrayDeque<FilterNode> nodes = new ArrayDeque<FilterNode>();
        pushToChain(nodes);

        List<String> fieldNames = new ArrayList<String>();

        FilterNode first = nodes.getFirst();
        while (!first.isFieldComparisonNode()) {
            nodes.removeFirst();
            first = nodes.getFirst();
        }

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
                } else if (filterNode.isDataInstanceNode()) {
                    return filterNode.getDataInstanceString();
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
        while (!first.isFieldComparisonNode()) {
            nodes.removeFirst();
            first = nodes.getFirst();
        }
        String firstNodeString = first.getNode() == null ? null : first.getNode().toOpenLString();
        sb.append("((Object[]) ").append(firstNodeString).append(") [");
        sb.append("(o) @ ");

        boolean multipleConditions = false;

        FilterNode node = nodes.pop();
        while (node != null && node.isFieldComparisonNode()) {
            if (multipleConditions) {
                sb.append(" and ");
            } else {
                multipleConditions = true;
            }

            Comparison comparison = node.getComparison();
            if (comparison == Comparison.NONE) {
                comparison = Comparison.EQUAL;
            }

            sb.append("Field(o, \"").append(node.getFieldName()).append("\")");
            sb.append(" ").append(comparison.getValue()).append(" ");
            sb.append(getConditionValueString(node));

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
                fields = fields.replace(firstNodeString == null ? "null" : firstNodeString, "o"); //FIXME
                sb.append("((Object[]) ").append(fields);

                sb.append(") [");
                sb.append("(o").append(varNumber).append(") @ ");

                Comparison comparison = node.getComparison();
                if (comparison == Comparison.NONE) {
                    comparison = Comparison.EQUAL;
                }

                sb.append("Field(o").append(varNumber).append(", \"").append(node.getFieldName()).append("\")");
                sb.append(" ").append(comparison.getValue()).append(" ");
                sb.append(getConditionValueString(node));

                sb.append("]"); // (Object[])

                sb.append(")"); // !isEmpty(

                varNumber++;
            }
            node = nodes.size() > 0 ? nodes.pop() : null;
        }

        sb.append("]");

        return sb.toString();
    }

    private String getConditionValueString(FilterNode node) {
        // TODO At this time OpenL doesn't support comparisons like a == 2 where a is "2". Remove the code below when it will be implemented
        Node value = node.getConditionValue();
        String valueString = value.toOpenLString();
        if (value instanceof NumberNode) {
            return "\"" + ((NumberNode) value).asString() + "\"";
        }
        return valueString;
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
            } else if (filterNode.isDataInstanceNode()) {
                obj = filterNode.getDataInstanceString();
            } else {
                log.warn("Unsupported type of node " + node.toOpenLString() + ". Skip it");
                obj = node.toOpenLString();
            }
        } else {
            obj = node == null ? null : node.toOpenLString();
        }

        if (skipFieldsCount > 0) {
            return obj;
        } else {
            if (conditionValue != null) {
                return "Field(" + obj + ", \"" + fieldName + "\", " + conditionValue.toOpenLString() + ")";
            } else {
                return "Field(" + obj + ", \"" + fieldName + "\")";
            }
        }
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

        throw new IllegalArgumentException("Can't apply Parent() to the node " + (node == null ?
                                                                                  "null" :
                                                                                  node.toOpenLString()));
    }

    protected String getDataInstanceString() {
        String dataInstanceName = "All" + fieldName;
        return node == null ? dataInstanceName : dataInstanceName + "[" + (Integer.parseInt(((NumberNode)node).asString()) - 1) + "]";
    }

    protected boolean isDataInstanceNode() {
        return isEmptyComparison() && (node == null || node instanceof NumberNode) && ProjectData.getCurrentInstance()
                .getTypeNames()
                .contains(fieldName);
    }

    protected boolean isFieldComparisonNode() {
        return node != null && hasComparison() && ProjectData.getCurrentInstance().getFieldNames().contains(fieldName);
    }

    protected boolean isFieldNode() {
        return node != null && (isEmptyComparison() || isIndex()) && ProjectData.getCurrentInstance()
                .getFieldNames()
                .contains(fieldName);
    }

    protected boolean isParentNode() {
        return node != null && node instanceof FilterNode && isEmptyComparison() && ProjectData.getCurrentInstance()
                .getTypeNames()
                .contains(fieldName);
    }

    protected void pushToChain(Deque<FilterNode> nodes) {
        nodes.push(this);
        if (getNode() instanceof FilterNode) {
            FilterNode node = (FilterNode) getNode();
            if (!node.isDataInstanceNode()) {
                node.pushToChain(nodes);
            }
        }
    }

    private boolean hasComparison() {
        return comparison != null;
    }

    private boolean isEmptyComparison() {
        return (comparison == null || comparison == Comparison.NONE) && conditionValue == null;
    }

    private boolean isIndex() {
        return comparison == null && conditionValue != null;
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
