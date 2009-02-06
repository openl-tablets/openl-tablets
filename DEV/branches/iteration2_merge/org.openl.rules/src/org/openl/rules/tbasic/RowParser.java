package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;

public class RowParser implements IRowParser {
    List<AlgorithmRow> rows;
    TableParserSpecificationBean[] specifications;

    public RowParser(List<AlgorithmRow> rows, TableParserSpecificationBean[] specifications) {
        assert rows != null;
        assert specifications != null;

        this.rows = rows;
        this.specifications = specifications;
    }

    public List<AlgorithmTreeNode> parse() throws Exception {
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
            TableParserSpecificationBean specification = getSpecification(operation.getValue(), multiline);
            if (validateRow(row, multiline, specification)) {
                StringValue[] nodeLabels = asNodeLabels(topLabels, label);
                AlgorithmTreeNode node = createAlgorithmNode(row, nodeLabels, specification, null);
                if (indent == 0) {
                    treeNodes.add(node);
                    if (parentTree.size() > 1) {
                        parentTree.clear();
                    }
                } else {
                    checkError(indent > (prevIndent + 1), "");
                    checkError(parentTree.isEmpty(), "");
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

    private void checkError(boolean errorCondition, String errorMessage) throws BoundError {
        if (errorCondition) {
            throw new BoundError(errorMessage == null ? "" : errorMessage, null);
        }
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

    private boolean validateRow(AlgorithmRow row,
            boolean multiline, TableParserSpecificationBean specification) throws BoundError {
        checkError(specification == null, "");
        checkError(specification.isObligatoryLabel() && row.getLabel().isEmpty(), "");
        checkError(specification.isMustHaveCondition() == row.getCondition().isEmpty(), "");
        checkError(specification.isMustHaveAction() == row.getAction().isEmpty(), "");
        checkError(!specification.isCanHaveBeforeAndAfter()
                && (!row.getBefore().isEmpty() || !row.getAfter().isEmpty()), "");
        checkError(specification.isCanBeOnlyTopLevel() && row.getOperationLevel() > 0, "");
        checkError(!specification.isCanHaveIdents() && multiline, "");
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
            String keyword, boolean multiline) {
        for (TableParserSpecificationBean specification : specifications) {
            String specKeyword = specification.getKeyword();
            boolean specMultiline = specification.isMultiLine();
            if (keyword.equalsIgnoreCase(specKeyword)
                    && multiline == specMultiline) {
                return specification;
            }
        }
        return null;
    }

}
