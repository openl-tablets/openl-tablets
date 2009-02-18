package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.TableParserSpecificationBean.ValueNecessity;

public class RowParser implements IRowParser {
    List<AlgorithmRow> rows;
    TableParserSpecificationBean[] specifications;

    public RowParser(List<AlgorithmRow> rows, TableParserSpecificationBean[] specifications) {
        assert rows != null;
        assert specifications != null;

        this.rows = rows;
        this.specifications = specifications;
    }

    public List<AlgorithmTreeNode> parse() throws BoundError {
        List<AlgorithmTreeNode> treeNodes = new ArrayList<AlgorithmTreeNode>();
        Map<Integer, AlgorithmTreeNode> parentTree = new HashMap<Integer, AlgorithmTreeNode>();
        int i = 0;
        int prevIndent = 0;
        List<StringValue> topLabels = new ArrayList<StringValue>();
        for (AlgorithmRow row : rows) {
            StringValue operation = row.getOperation();
            StringValue label = row.getLabel();
            if (operation.isEmpty()) {
                if (!label.isEmpty()) {
                    topLabels.add(label);
                }
                i++;
                continue;
            }
            int indent = row.getOperationLevel();
            boolean multiline = isMultiline(i, indent);
            TableParserSpecificationBean specification = getSpecification(operation, multiline);
            if (validateRow(row, multiline, specification)) {
                StringValue[] nodeLabels = asNodeLabels(topLabels, label);
                AlgorithmTreeNode node = createAlgorithmNode(row, nodeLabels, specification, null);
                if (indent == 0) {
                    treeNodes.add(node);
                    if (parentTree.size() > 1) {
                        parentTree.clear();
                    }
                } else {
                    checkError(indent > (prevIndent + 1), operation,
                            "Incorrect operation indention. Expected indention is " + (prevIndent + 1));
                    checkError(parentTree.isEmpty(), operation,
                            "Incorrect operation indention. Could not find parent operation with 0 indention");
                    addChild(parentTree.get(indent - 1), node);
                }
                parentTree.put(indent, node);
                prevIndent = indent;
            }
            i++;
        }
        return treeNodes;
    }

    private StringValue[] asNodeLabels(List<StringValue> topLabels, StringValue currentLabel) {
        StringValue[] nodeLabels = null;
        if (!topLabels.isEmpty()) {
            if (!currentLabel.isEmpty()) {
                topLabels.add(currentLabel);
            }
            nodeLabels = new StringValue[topLabels.size()];
            nodeLabels = topLabels.toArray(nodeLabels);
        } else {
            nodeLabels = new StringValue[] { currentLabel };
        }
        topLabels.clear();
        return nodeLabels;
    }

    private boolean isMultiline(int rowIndex, int rowIndent) {
        boolean multiline = false;
        if (rowIndex < rows.size() - 1) {
            int nextRowIndent = rows.get(rowIndex + 1).getOperationLevel();
            multiline = nextRowIndent > rowIndent;
        }
        return multiline;
    }

    private void checkError(boolean errorCondition, StringValue srcValue,
            String errorMessage) throws BoundError {
        if (errorCondition) {
            throw new BoundError(errorMessage == null ? "" : errorMessage,
                    srcValue.asSourceCodeModule());
        }
    }

    private AlgorithmTreeNode addChild(AlgorithmTreeNode parent, AlgorithmTreeNode child) {
        List<AlgorithmTreeNode> children = parent.getChildren();
        if (children == null) {
            children = new ArrayList<AlgorithmTreeNode>();
            parent.setChildren(children);
        }
        children.add(child);
        return parent;
    }

    private boolean validateRow(AlgorithmRow row, boolean multiline,
            TableParserSpecificationBean spec) throws BoundError {
        // check Label
        checkError(spec.getLabel() == ValueNecessity.REQUIRED && row.getLabel().isEmpty(), row.getLabel(),
                "Label is empty. Label is obligatory for this operation");
        // check Condition
        StringValue condition = row.getCondition();
        ValueNecessity specCondition = spec.getCondition();
        checkError(spec.getCondition() == ValueNecessity.REQUIRED && condition.isEmpty(), condition,
                "Operation must have Condition value");
        checkError(spec.getCondition() == ValueNecessity.PROHIBITED && !condition.isEmpty(), condition,
                "Operation must not have Condition value");
        // check Action
        StringValue action = row.getAction();
        ValueNecessity specAction = spec.getAction();
        checkError(specAction == ValueNecessity.REQUIRED && action.isEmpty(), action,
                "Operation must have Action value");
        checkError(specAction == ValueNecessity.PROHIBITED && !action.isEmpty(), action,
                "Operation must not have Action value");
        // check Before
        StringValue before = row.getBefore();
        ValueNecessity specBeforeAfter = spec.getBeforeAndAfter();
        checkError(specBeforeAfter == ValueNecessity.REQUIRED && before.isEmpty(), before,
                "Operation must have Before value");
        checkError(specBeforeAfter == ValueNecessity.PROHIBITED && !before.isEmpty(), before,
                "Operation must not have Before value");
        // check After
        StringValue after = row.getAfter();
        checkError(specBeforeAfter == ValueNecessity.REQUIRED && after.isEmpty(), after,
                "Operation must have After value");
        checkError(specBeforeAfter == ValueNecessity.PROHIBITED && !after.isEmpty(), after,
                "Operation must not have After value");
        // check Top Level
        StringValue operation = row.getOperation();
        int indent = row.getOperationLevel();
        ValueNecessity specTopLevel = spec.getTopLevel();
        checkError(specTopLevel == ValueNecessity.PROHIBITED && indent > 0, operation,
                "Operation can not be a top level, i.e. must be nested");
        checkError(specTopLevel == ValueNecessity.REQUIRED && indent > 0, operation,
                "Operation can be only a top level, i.e. can not be nested");
        return true;
    }

    private AlgorithmTreeNode createAlgorithmNode(AlgorithmRow row, StringValue[] labels,
            TableParserSpecificationBean specification, List<AlgorithmTreeNode> children) {
        AlgorithmTreeNode node = new AlgorithmTreeNode();
        node.setAlgorithmRow(row);
        node.setLabels(labels);
        node.setSpecification(specification);
        node.setChildren(children);
        return node;
    }

    private TableParserSpecificationBean getSpecification(
            StringValue operation, boolean multiline) throws BoundError {
        boolean found = false;
        for (TableParserSpecificationBean specification : specifications) {
            String specKeyword = specification.getKeyword();
            if (operation.getValue().equalsIgnoreCase(specKeyword)) {
                boolean specMultiline = specification.isMultiline();
                if (specMultiline == multiline) {
                    return specification;
                }
                found = true;
            }
        }
        checkError(found, operation,
                "Operation can not be multiline, i.e. can not have nested operations");
        checkError(true, operation, "No such operation: " + operation.getValue());
        return null;
    }

}
