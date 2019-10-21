package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.TableParserSpecificationBean.ValueNecessity;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class RowParser implements IRowParser {
    private static final String COMMENTS_REGEXP = "^(//)(.*)|^(/\\*)(.*)(\\*/)$";
    private static final String CONDITION = "Condition";
    private static final String ACTION = "Action";
    private static final String BEFORE = "Before";
    private static final String AFTER = "After";
    private List<AlgorithmRow> rows;
    private TableParserSpecificationBean[] specifications;

    public RowParser(List<AlgorithmRow> rows, TableParserSpecificationBean[] specifications) {
        assert rows != null;
        assert specifications != null;

        this.rows = rows;
        this.specifications = specifications;
    }

    private void checkRowValue(StringValue operation,
            String columnName,
            StringValue columnValue,
            ValueNecessity columnNecessity) throws SyntaxNodeException {

        if (columnNecessity == ValueNecessity.REQUIRED && columnValue.isEmpty()) {
            IOpenSourceCodeModule source = columnValue.asSourceCodeModule();
            if (source.getUri() == null) {
                // Column <columnName> is absent. Point to <operation> cell instead.
                String errMsg = String.format("%s is required for operation '%s'!", columnName, operation);
                throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
            } else {
                // Column <columnName> exists but still is empty. Point to empty <columnValue> cell.
                String errMsg = String.format("Operation must have value in %s!", columnName);
                throw SyntaxNodeExceptionUtils.createError(errMsg, source);
            }
        }

        if (columnNecessity == ValueNecessity.PROHIBITED && !columnValue.isEmpty()) {
            String errMsg = String.format("Operation must not have value in %s!", columnName);
            throw SyntaxNodeExceptionUtils.createError(errMsg, columnValue.asSourceCodeModule());
        }
    }

    private TableParserSpecificationBean getSpecification(StringValue operation,
            boolean multiline) throws SyntaxNodeException {
        String operationName = operation.getValue();
        boolean foundButNotMatch = false;
        for (TableParserSpecificationBean specification : specifications) {
            String specKeyword = specification.getKeyword();
            if (operationName.equalsIgnoreCase(specKeyword)) {
                if (specification.isMultiline() == multiline) {
                    return specification;
                }
                foundButNotMatch = true;
            }
        }

        if (foundButNotMatch) {
            String errorMessage;
            if (multiline) {
                // If operation is used as multiline and it does not match the specification the error should be next
                //
                errorMessage = "Operation %s cannot be multiline! Nested operations are not allowed here.";
            } else {
                // If the operation is used as single line and it does not match the specification, the error should be
                // next
                //
                errorMessage = "Operation %s cannot be singleline!";
            }
            throw SyntaxNodeExceptionUtils.createError(String.format(errorMessage, operationName),
                operation.asSourceCodeModule());
        }

        String errMsg = "No such operation: " + operationName;
        throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
    }

    /**
     * Guess by the number of the modes in the list and the operation level of each node (aka indent) if the operation
     * is multiline or not
     */
    private boolean[] guessMultiline(List<AlgorithmTreeNode> nodes) {
        int size = nodes.size();
        boolean[] multilines = new boolean[size];
        for (int i = 0; i < size - 1; i++) {
            AlgorithmTreeNode node = nodes.get(i);
            AlgorithmRow row = node.getAlgorithmRow();
            int i1 = row.getOperationLevel();

            AlgorithmTreeNode nextNode = nodes.get(i + 1);
            AlgorithmRow nextRow = nextNode.getAlgorithmRow();
            int i2 = nextRow.getOperationLevel();

            multilines[i] = (i1 < i2);
        }

        return multilines;
    }

    @Override
    public List<AlgorithmTreeNode> parse() throws SyntaxNodeException {
        List<AlgorithmTreeNode> nodes = prepareNodes();

        // TODO: refactor. Create AlgorithmNodeWithGuess decorator over the AlgorithmTreeNode
        // and work with this entity
        //
        boolean[] guessedMultilines = guessMultiline(nodes);

        List<AlgorithmTreeNode> treeNodes = new ArrayList<>();
        Map<Integer, AlgorithmTreeNode> parentTree = new HashMap<>();

        int prevIndent = 0;
        for (int i = 0; i < nodes.size(); i++) {
            AlgorithmTreeNode node = nodes.get(i);
            AlgorithmRow row = node.getAlgorithmRow();

            TableParserSpecificationBean specification = validateRow(row, guessedMultilines[i]);
            node.setSpecification(specification);

            int indent = row.getOperationLevel();
            if (indent == 0) {
                treeNodes.add(node);
                parentTree.clear();
            } else {
                StringValue operation = row.getOperation();
                if (indent > (prevIndent + 1)) {
                    String errMsg = String.format("Incorrect operation indention! Expected %d.", (prevIndent + 1));
                    throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
                }
                if (parentTree.isEmpty()) {
                    String errMsg = "Incorrect operation indention! Could not find parent operation with 0 indention.";
                    throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
                }

                parentTree.get(indent - 1).add(node);
            }
            parentTree.put(indent, node);
            prevIndent = indent;
        }

        return treeNodes;
    }

    private List<AlgorithmTreeNode> prepareNodes() {
        // cut off commented rows, pack labels
        List<AlgorithmTreeNode> nodes = new ArrayList<>();

        AlgorithmTreeNode lastNode = new AlgorithmTreeNode();
        for (AlgorithmRow row : rows) {
            StringValue operation = row.getOperation();
            StringValue label = row.getLabel();

            if (operation == null) {
                throw new OpenlNotCheckedException(
                    String.format("There is no operations in row '%s'", row.getDescription()));
            }

            if (operation.isEmpty()) {
                if (!label.isEmpty()) {
                    // stack up labels
                    lastNode.addLabel(label);
                }
            } else if (operation.getValue().matches(COMMENTS_REGEXP)) {
                // ignore
            } else {
                // has some operation
                if (!label.isEmpty()) {
                    lastNode.addLabel(label);
                } else {
                    // if no labels at all
                    if (lastNode.getLabels().isEmpty()) {
                        // add this empty label anyway
                        lastNode.addLabel(label);
                    }
                }

                lastNode.setAlgorithmRow(row);
                nodes.add(lastNode);
                lastNode = new AlgorithmTreeNode();
            }
        }

        if (lastNode.getAlgorithmRow() != null) {
            nodes.add(lastNode);
        }

        return nodes;
    }

    private TableParserSpecificationBean validateRow(AlgorithmRow row,
            boolean guessedMultiline) throws SyntaxNodeException {
        StringValue operation = row.getOperation();
        TableParserSpecificationBean spec = getSpecification(operation, guessedMultiline);

        // check Label
        if (spec.getLabel() == ValueNecessity.REQUIRED && row.getLabel().isEmpty()) {
            String errMsg = "Label is obligatory for this operation!";
            throw SyntaxNodeExceptionUtils.createError(errMsg, row.getLabel().asSourceCodeModule());
        }

        checkRowValue(operation, CONDITION, row.getCondition(), spec.getCondition());
        checkRowValue(operation, ACTION, row.getAction(), spec.getAction());
        checkRowValue(operation, BEFORE, row.getBefore(), spec.getBeforeAndAfter());
        checkRowValue(operation, AFTER, row.getAfter(), spec.getBeforeAndAfter());

        // check Top Level
        int indent = row.getOperationLevel();
        ValueNecessity specTopLevel = spec.getTopLevel();
        if (specTopLevel == ValueNecessity.PROHIBITED && indent == 0) {
            throw SyntaxNodeExceptionUtils.createError("Operation cannot be a top level element! It should be nested.",
                operation.asSourceCodeModule());
        }
        if (specTopLevel == ValueNecessity.REQUIRED && indent > 0) {
            throw SyntaxNodeExceptionUtils.createError("Operation can be a top level only!",
                operation.asSourceCodeModule());
        }

        // passed
        return spec;
    }
}
