package org.openl.rules.cmatch.algorithm;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableColumn;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.rules.cmatch.matcher.MatcherFactory;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.types.IOpenClass;

public class MatchAlgorithmCompiler implements IMatchAlgorithmCompiler {
    public static final String NAMES = "names";
    public static final String OPERATION = "operation";
    public static final String VALUES = "values";

    public static final String ROW_RET_VALUE = "Return Values";

    protected static final List<ColumnDefinition> MATCH_COLUMN_DEFINITION = new LinkedList<ColumnDefinition>();
    static {
        MATCH_COLUMN_DEFINITION.add(new ColumnDefinition(NAMES, false));
        MATCH_COLUMN_DEFINITION.add(new ColumnDefinition(OPERATION, false));
        MATCH_COLUMN_DEFINITION.add(new ColumnDefinition(VALUES, true));
    }

    private static final MatchAlgorithmExecutor EXECUTOR = new MatchAlgorithmExecutor();

    public void compile(IBindingContext bindingContext, ColumnMatch columnMatch) throws BoundError {
        checkReqColumns(columnMatch.getColumns());
        checkRows(columnMatch.getRows());

        int minRows = getSpecialRowCount() + 1;
        if (columnMatch.getRows().size() < minRows) {
            String msg = "Expects at least " + minRows + " rows!";
            throw new IllegalArgumentException(msg);
        }

        checkSpecialRows(columnMatch);

        parseSpecialRows(columnMatch);

        ArgumentsHelper argumentsHelper = new ArgumentsHelper(columnMatch.getHeader().getSignature());

        // [0..X] special rows are ignored
        List<TableRow> rows = columnMatch.getRows();
        MatchNode[] nodes = prepareNodes(columnMatch, argumentsHelper, columnMatch.getReturnValues().length);

        MatchNode rootNode = buildTree(rows, nodes);
        validateTree(rootNode, rows, nodes);
        columnMatch.setCheckTree(rootNode);

        assignExecutor(columnMatch);
    }

    protected List<ColumnDefinition> getColumnDefinition() {
        return MATCH_COLUMN_DEFINITION;
    }

    /**
     * Checks that all required columns are defined.
     * 
     * @param columns
     * @see #getRequiredColumns()
     */
    private void checkReqColumns(List<TableColumn> columns) {
        for (ColumnDefinition colDef : getColumnDefinition()) {
            boolean exists = false;
            for (TableColumn column : columns) {
                if (colDef.getName().equals(column.getId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                throw new IllegalArgumentException("Required column " + colDef.getName() + " is absent!");
            }
        }
    }

    private void checkRows(List<TableRow> rows) {
        for (int i = 0; i < rows.size(); i++) {
            TableRow row = rows.get(i);

            for (ColumnDefinition colDef : getColumnDefinition()) {
                checkColumnValue(row, colDef);
            }
        }
    }

    protected void checkSpecialRows(ColumnMatch columnMatch) throws BoundError {
        List<TableRow> rows = columnMatch.getRows();
        checkRowName(rows.get(0), ROW_RET_VALUE);
    }

    protected void checkRowName(TableRow row, String expectedName) throws BoundError {
        SubValue sv = row.get(NAMES)[0];
        if (!expectedName.equalsIgnoreCase(sv.getString())) {
            String msg = "Expects " + expectedName + " here!";
            throw new BoundError(msg, sv.getStringValue().asSourceCodeModule());
        }
    }

    protected int getSpecialRowCount() {
        return 1;
    }

    protected void checkColumnValue(TableRow row, ColumnDefinition colDef) {
        SubValue[] values = row.get(colDef.getName());
        if (!colDef.isMultipleValueAllowed()) {
            // only 1
            if (values.length != 1) {
                throw new IllegalArgumentException("Column " + colDef.getName() + " can have single value only!");
            }
        }
    }

    /**
     * Compiles (parses) return values based on return type.
     * 
     * @param columnMatch
     */
    protected void parseSpecialRows(ColumnMatch columnMatch) throws BoundError {
        IOpenClass returnType = columnMatch.getHeader().getType();
        Object[] retValues = parseValues(columnMatch.getRows().get(0), returnType.getInstanceClass());
        columnMatch.setReturnValues(retValues);
    }

    protected Object[] parseValues(TableRow row, Class<?> clazz) {
        IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(clazz);

        SubValue[] subValues = row.get(VALUES);

        Object[] result = new Object[subValues.length];
        for (int i = 0; i < subValues.length; i++) {
            SubValue sv = subValues[i];
            String s = sv.getString();

            result[i] = converter.parse(s, null, null);
        }

        return result;
    }

    /**
     * Prepares Nodes. Check names, operations and assigns matchers.
     * <p>
     * Special rows are ignored. That is why first n elements in return array is
     * always null.
     * 
     * @param rows
     * @param argumentsHelper
     * @return array of nodes, elements corresponds rows
     * @throws BoundError
     */
    protected MatchNode[] prepareNodes(ColumnMatch columnMatch, ArgumentsHelper argumentsHelper, int retValuesCount)
            throws BoundError {
        List<TableRow> rows = columnMatch.getRows();
        MatchNode[] nodes = new MatchNode[rows.size()];

        for (int i = getSpecialRowCount(); i < rows.size(); i++) {
            TableRow row = rows.get(i);
            SubValue nameSV = row.get(NAMES)[0];
            String varName = nameSV.getString();

            Argument arg = argumentsHelper.getTypeByName(varName);
            if (arg == null) {
                String msg = "Failed to bind " + varName + "!";
                throw new BoundError(msg, nameSV.getStringValue().asSourceCodeModule());
            }

            SubValue operationSV = row.get(OPERATION)[0];
            String operationName = operationSV.getString();

            IMatcher matcher = MatcherFactory.getMatcher(operationName, arg.getType());
            if (matcher == null) {
                String msg = "No matcher was found for operation " + operationName + " and type " + arg.getType();
                throw new BoundError(msg, operationSV.getStringValue().asSourceCodeModule());
            }

            MatchNode node = new MatchNode(i);
            node.setMatcher(matcher);
            node.setArgument(arg);

            parseCheckValues(row, node, retValuesCount);

            nodes[i] = node;
        }

        return nodes;
    }

    /**
     * Builds tree based on indentation of each row.
     * 
     * @param rows
     * @param nodes
     *            (special rows must be null)
     * @return root of tree
     * @throws BoundError
     */
    protected MatchNode buildTree(List<TableRow> rows, MatchNode[] nodes) throws BoundError {
        MatchNode rootNode = new MatchNode(-1);

        MatchNode[] lastForIndent = new MatchNode[nodes.length];
        int prevIndent = 0;
        for (int i = getSpecialRowCount(); i < rows.size(); i++) {
            MatchNode node = nodes[i];
            TableRow row = rows.get(i);
            SubValue nameSV = row.get(NAMES)[0];
            int indent = nameSV.getIndent();

            if (indent == 0) {
                rootNode.add(node);
            } else {
                if (indent == (prevIndent + 1)) {
                    lastForIndent[prevIndent].add(node);
                } else if (indent > (prevIndent + 1)) {
                    // can increase by +1 only
                    String msg = MessageFormat.format("Illegal indent! 0..{0} expected.", prevIndent + 1);
                    throw new BoundError(msg, nameSV.getStringValue().asSourceCodeModule());
                } else {
                    // if (indent == prevIndent)
                    // if (indent 1..prevIndent-1)
                    lastForIndent[indent].getParent().add(node);
                }
            }

            prevIndent = indent;
            lastForIndent[indent] = node;
        }

        return rootNode;
    }

    /**
     * Checks that tree is consistent.
     * 
     * @param rootNode
     *            root of tree
     * @param rows
     *            rows to point out errors
     * @throws BoundError
     */
    protected void validateTree(MatchNode rootNode, List<TableRow> rows, MatchNode[] nodes) throws BoundError {
        for (MatchNode node : rootNode.getChildren()) {
            if (node.isLeaf()) {
                // ok
            } else {
                // has at least 1 child
                checkTreeChildren(node, rows);
            }
        }

        linearizeTree(rootNode, nodes);
    }

    private void checkTreeChildren(MatchNode parent, List<TableRow> rows) throws BoundError {
        int childCount = 0;
        int childLeafs = 0;
        for (MatchNode child : parent.getChildren()) {
            if (child.isLeaf())
                childLeafs++;
            childCount++;
        }

        if (childCount == childLeafs) {
            // No children or all are leafs
        } else if (childCount == 1 && childLeafs == 0) {
            // check child
            for (MatchNode child : parent.getChildren()) {
                checkTreeChildren(child, rows);
            }
        } else {
            String msg = "All sub nodes must be leaves! Sub nodes are allowed for single child only.";
            throw new BoundError(msg, rows.get(parent.getRowIndex()).get(NAMES)[0].getStringValue()
                    .asSourceCodeModule());
        }
    }

    private void linearizeTree(MatchNode rootNode, MatchNode[] nodes) {
        rootNode.clearChildren();

        MatchNode last0 = null;
        for (int i = getSpecialRowCount(); i < nodes.length; i++) {
            MatchNode node = nodes[i];
            if (node.getParent() == rootNode) {
                last0 = new MatchNode(-2);
                last0.setWeight(node.getWeight());
                // use synthetic node
                rootNode.add(last0);
            }

            last0.add(node);
        }
    }

    /**
     * Parses CheckValues for node(row). It is up to matcher (type of variable
     * in 'names') how to parse it.
     * 
     * @param row
     * @param node
     * @param retValuesCount
     */
    protected void parseCheckValues(TableRow row, MatchNode node, int retValuesCount) {
        SubValue[] inValues = row.get(VALUES);
        Object[] checkValues = new Object[retValuesCount];

        IMatcher matcher = node.getMatcher();
        for (int index = 0; index < inValues.length; index++) {
            String s = inValues[index].getString().trim();

            if (s.length() > 0) {
                // ignore empty cells
                Object v = matcher.fromString(s);
                checkValues[index] = v;
            }
        }

        node.setCheckValues(checkValues);
    }

    protected void assignExecutor(ColumnMatch columnMatch) {
        columnMatch.setAlgorithmExecutor(EXECUTOR);
    }
}
