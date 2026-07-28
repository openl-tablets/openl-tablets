package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.TableParserSpecificationBean.ValueNecessity;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

public class RowParser implements IRowParser {
    private static final String COMMENTS_REGEXP = "^(//)(.*)|^(/\\*)(.*)(\\*/)$";
    private static final String CONDITION = "Condition";
    private static final String ACTION = "Action";
    private static final String BEFORE = "Before";
    private static final String AFTER = "After";
    private final List<AlgorithmRow> rows;
    private final TableParserSpecificationBean[] specifications;

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
            var source = columnValue.asSourceCodeModule();
            if (source.getUri() == null) {
                // Column <columnName> is absent. Point to <operation> cell instead.
                var errMsg = "%s is required for operation %s.".formatted(columnName, operation);
                throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
            } else {
                // Column <columnName> exists but still is empty. Point to empty <columnValue> cell.
                var errMsg = "Operation must have value in %s.".formatted(columnName);
                throw SyntaxNodeExceptionUtils.createError(errMsg, source);
            }
        }

        if (columnNecessity == ValueNecessity.PROHIBITED && !columnValue.isEmpty()) {
            var errMsg = "Operation must not have value in %s.".formatted(columnName);
            throw SyntaxNodeExceptionUtils.createError(errMsg, columnValue.asSourceCodeModule());
        }
    }

    private TableParserSpecificationBean getSpecification(StringValue operation,
                                                          boolean multiline) throws SyntaxNodeException {
        var operationName = operation.getValue();
        var foundButNotMatch = false;
        for (TableParserSpecificationBean specification : specifications) {
            var specKeyword = specification.getKeyword();
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
                errorMessage = "Operation %s cannot be multiline. Nested operations are not allowed here.";
            } else {
                // If the operation is used as single line and it does not match the specification, the error should be
                // next
                //
                errorMessage = "Operation %s cannot be singleline.";
            }
            throw SyntaxNodeExceptionUtils.createError(errorMessage.formatted(operationName),
                    operation.asSourceCodeModule());
        }

        var errMsg = "No such operation: " + operationName;
        throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
    }

    /**
     * Guess by the number of the modes in the list and the operation level of each node (aka indent) if the operation
     * is multiline or not
     */
    private boolean[] guessMultiline(List<AlgorithmTreeNode> nodes) {
        var size = nodes.size();
        boolean[] multilines = new boolean[size];
        for (var i = 0; i < size - 1; i++) {
            var node = nodes.get(i);
            var row = node.getAlgorithmRow();
            var i1 = row.getOperationLevel();

            var nextNode = nodes.get(i + 1);
            var nextRow = nextNode.getAlgorithmRow();
            var i2 = nextRow.getOperationLevel();

            multilines[i] = i1 < i2;
        }

        return multilines;
    }

    @Override
    public List<AlgorithmTreeNode> parse() throws SyntaxNodeException {
        List<AlgorithmTreeNode> nodes = prepareNodes();

        // TODO: refactor. Create AlgorithmNodeWithGuess decorator over the AlgorithmTreeNode
        // and work with this entity
        //
        var guessedMultilines = guessMultiline(nodes);

        var treeNodes = new ArrayList<AlgorithmTreeNode>();
        var parentTree = new HashMap<Integer, AlgorithmTreeNode>();

        var prevIndent = 0;
        for (var i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            var row = node.getAlgorithmRow();

            var specification = validateRow(row, guessedMultilines[i]);
            node.setSpecification(specification);

            var indent = row.getOperationLevel();
            if (indent == 0) {
                treeNodes.add(node);
                parentTree.clear();
            } else {
                var operation = row.getOperation();
                if (indent > prevIndent + 1) {
                    var errMsg = "Incorrect operation indention! Expected %d.".formatted(prevIndent + 1);
                    throw SyntaxNodeExceptionUtils.createError(errMsg, operation.asSourceCodeModule());
                }
                if (parentTree.isEmpty()) {
                    var errMsg = "Incorrect operation indention! Could not find parent operation with 0 indention.";
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
        var nodes = new ArrayList<AlgorithmTreeNode>();

        var lastNode = new AlgorithmTreeNode();
        for (AlgorithmRow row : rows) {
            var operation = row.getOperation();
            var label = row.getLabel();

            if (operation == null) {
                throw new OpenlNotCheckedException(
                        "There is no operations in row '%s'".formatted(row.getDescription()));
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
        var operation = row.getOperation();
        var spec = getSpecification(operation, guessedMultiline);

        // check Label
        if (spec.getLabel() == ValueNecessity.REQUIRED && row.getLabel().isEmpty()) {
            var errMsg = "Label is obligatory for this operation.";
            throw SyntaxNodeExceptionUtils.createError(errMsg, row.getLabel().asSourceCodeModule());
        }

        checkRowValue(operation, CONDITION, row.getCondition(), spec.getCondition());
        checkRowValue(operation, ACTION, row.getAction(), spec.getAction());
        checkRowValue(operation, BEFORE, row.getBefore(), spec.getBeforeAndAfter());
        checkRowValue(operation, AFTER, row.getAfter(), spec.getBeforeAndAfter());

        // check Top Level
        var indent = row.getOperationLevel();
        var specTopLevel = spec.getTopLevel();
        if (specTopLevel == ValueNecessity.PROHIBITED && indent == 0) {
            throw SyntaxNodeExceptionUtils.createError("Operation cannot be a top level element! It should be nested.",
                    operation.asSourceCodeModule());
        }
        if (specTopLevel == ValueNecessity.REQUIRED && indent > 0) {
            throw SyntaxNodeExceptionUtils.createError("Operation can be a top level only.",
                    operation.asSourceCodeModule());
        }

        // passed
        return spec;
    }
}
