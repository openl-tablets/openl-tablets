package org.openl.rules.cmatch.algorithm;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableColumn;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.matcher.IMatcher;
import org.openl.rules.cmatch.matcher.MatcherFactory;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

public class MatchAlgorithmCompiler implements IMatchAlgorithmCompiler {
    public static final String NAMES = "names";
    public static final String OPERATION = "operation";
    public static final String VALUES = "values";

    public static final String ROW_RET_VALUE = "Return Values";

    protected static final List<ColumnDefinition> MATCH_COLUMN_DEFINITION = new LinkedList<ColumnDefinition>();
    private static final MatchAlgorithmExecutor EXECUTOR = new MatchAlgorithmExecutor();

    static {
        MATCH_COLUMN_DEFINITION.add(new ColumnDefinition(NAMES, false));
        MATCH_COLUMN_DEFINITION.add(new ColumnDefinition(OPERATION, false));
        MATCH_COLUMN_DEFINITION.add(new ColumnDefinition(VALUES, true));
    }

    protected void assignExecutor(ColumnMatch columnMatch) {
        columnMatch.setAlgorithmExecutor(EXECUTOR);
    }

    /**
     * Sets CellMetaInfo by type of parsed value. Thus, editor can use special controller to validate/limit user input.
     * <p>
     * Null values from {@literal objValues} will be ignored. That let us use both numeric and range values.
     * <p>
     * Side effect: when matcher parse cell as numeric binding will block range there.
     */
    protected void bindMetaInfo(ColumnMatch columnMatch, String paramName, SubValue[] subValues, Object[] objValues) {
        IGridTable tableBodyGrid = columnMatch.getSyntaxNode().getTableBody().getSource();
        IGrid grid = tableBodyGrid.getGrid();

        // Bind cell data type based on parsed data
        for (int i = 0; i < subValues.length; i++) {
            IGridRegion gridRegion = subValues[i].getGridRegion();
            Object cv = objValues[i];

            if (cv != null && grid.getCell(gridRegion.getLeft(), gridRegion.getTop()).getMetaInfo() == null) {
                IOpenClass paramType = JavaOpenClass.getOpenClass(cv.getClass());
                CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, paramName, paramType, false);
                grid.getCell(gridRegion.getLeft(), gridRegion.getTop()).setMetaInfo(meta);
            }
            // empty cells are left 'as is' -- suppose they are of String type
        }
    }

    /**
     * Sets CellMetaInfo for 'names' column (except special rows).
     */
    protected void bindNamesMetaInfo(ColumnMatch columnMatch, ArgumentsHelper argumentsHelper) {
        DomainOpenClass domainOpenClass = argumentsHelper.generateDomainClassByArgNames();

        IGridTable tableBodyGrid = columnMatch.getSyntaxNode().getTableBody().getSource();
        IGrid grid = tableBodyGrid.getGrid();

        List<TableRow> rows = columnMatch.getRows();
        for (int i = getSpecialRowCount(); i < rows.size(); i++) {
            TableRow row = rows.get(i);
            SubValue nameSV = row.get(NAMES)[0];
            IGridRegion gridRegion = nameSV.getGridRegion();

            CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, null, domainOpenClass, false);
            grid.getCell(gridRegion.getLeft(), gridRegion.getTop()).setMetaInfo(meta);
        }
    }

    /**
     * Builds tree based on indentation of each row.
     *
     * @param nodes (special rows must be null)
     * @return root of tree
     */
    protected MatchNode buildTree(List<TableRow> rows, MatchNode[] nodes) throws SyntaxNodeException {
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
                    throw SyntaxNodeExceptionUtils.createError(msg, nameSV.getStringValue().asSourceCodeModule());
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
     * Checks that all required columns are defined.
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

    protected void checkRowName(TableRow row, String expectedName) throws SyntaxNodeException {
        SubValue sv = row.get(NAMES)[0];
        if (!expectedName.equalsIgnoreCase(sv.getString())) {
            String msg = "Expects " + expectedName + " here!";
            throw SyntaxNodeExceptionUtils.createError(msg, sv.getStringValue().asSourceCodeModule());
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

    protected void checkSpecialRows(ColumnMatch columnMatch) throws SyntaxNodeException {
        List<TableRow> rows = columnMatch.getRows();
        checkRowName(rows.get(0), ROW_RET_VALUE);
    }

    private void checkTreeChildren(MatchNode parent, List<TableRow> rows) throws SyntaxNodeException {
        int childCount = 0;
        int childLeafs = 0;
        for (MatchNode child : parent.getChildren()) {
            if (child.isLeaf()) {
                childLeafs++;
            }
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
            throw SyntaxNodeExceptionUtils.createError(msg,
                rows.get(parent.getRowIndex()).get(NAMES)[0].getStringValue().asSourceCodeModule());
        }
    }

    public void compile(IBindingContext bindingContext, ColumnMatch columnMatch) throws SyntaxNodeException {
        int minRows = getSpecialRowCount() + 1;
        if (columnMatch.getRows().size() < minRows) {
            String msg = "Expects at least " + minRows + " rows!";
            throw new IllegalArgumentException(msg);
        }

        checkReqColumns(columnMatch.getColumns());
        checkRows(columnMatch.getRows());
        checkSpecialRows(columnMatch);

        ArgumentsHelper argumentsHelper = new ArgumentsHelper(columnMatch.getHeader().getSignature());
        bindNamesMetaInfo(columnMatch, argumentsHelper);

        parseSpecialRows(bindingContext, columnMatch);
        // [0..X] special rows are ignored
        List<TableRow> rows = columnMatch.getRows();
        MatchNode[] nodes = prepareNodes(bindingContext,
            columnMatch,
            argumentsHelper,
            columnMatch.getReturnValues().length);

        MatchNode rootNode = buildTree(rows, nodes);
        validateTree(rootNode, rows, nodes);
        columnMatch.setCheckTree(rootNode);

        assignExecutor(columnMatch);
    }

    protected List<ColumnDefinition> getColumnDefinition() {
        return MATCH_COLUMN_DEFINITION;
    }

    protected int getSpecialRowCount() {
        return 1;
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
     * Parses CheckValues for node(row). It is up to matcher (type of variable in 'names') how to parse it.
     */
    protected void parseCheckValues(IBindingContext bindingContext, ColumnMatch columnMatch, TableRow row, MatchNode node, int retValuesCount) {
        SubValue[] inValues = row.get(VALUES);
        Object[] checkValues = new Object[retValuesCount];

        IMatcher matcher = node.getMatcher();
        for (int index = 0; index < inValues.length; index++) {
            String s = inValues[index].getString().trim();

            if (s.length() > 0) {
                // ignore empty cells
                Object v = null;
                ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, s);
                if (constantOpenField != null && constantOpenField.getValue() != null) {
                    IOpenClass type = null;
                    if (node.getArgument() != null) {
                        type = node.getArgument().getType();
                    }else {
                        type = JavaOpenClass.getOpenClass(Integer.class);
                    }
                    setMetaInfoForConstant(bindingContext, columnMatch, inValues[index], s, constantOpenField);
                    v = RuleRowHelper.castConstantToExpectedType(bindingContext, constantOpenField, type);
                } else {
                    v = matcher.fromString(s);
                }
                checkValues[index] = v;
            }
        }

        node.setCheckValues(checkValues);
    }

    /**
     * Compiles (parses) return values based on return type.
     */
    protected void parseSpecialRows(IBindingContext bindingContext, ColumnMatch columnMatch) throws SyntaxNodeException {
        IOpenClass returnType = columnMatch.getHeader().getType();

        TableRow row0 = columnMatch.getRows().get(0);
        Object[] retValues = parseValues(bindingContext, columnMatch, row0, returnType);
        columnMatch.setReturnValues(retValues);

        bindMetaInfo(columnMatch, "Return Values", row0.get(VALUES), retValues);
    }

    protected Object[] parseValues(IBindingContext bindingContext, ColumnMatch columnMatch, TableRow row, IOpenClass openClass) throws SyntaxNodeException {
        SubValue[] subValues = row.get(VALUES);

        Object[] result = new Object[subValues.length];
        for (int i = 0; i < subValues.length; i++) {
            SubValue sv = subValues[i];
            String s = sv.getString();

            try {
                ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, s);
                if (constantOpenField != null && constantOpenField.getValue() != null) {
                    setMetaInfoForConstant(bindingContext, columnMatch, sv, s, constantOpenField);
                    result[i] = RuleRowHelper.castConstantToExpectedType(bindingContext, constantOpenField, openClass);
                } else {
                    IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(openClass.getInstanceClass());
                    result[i] = converter.parse(s, null);
                }
            } catch (Exception | LinkageError ex) {
                TextInterval location = LocationUtils.createTextInterval(s);
                throw SyntaxNodeExceptionUtils.createError(ex, location, sv.getStringValue().asSourceCodeModule());
            } 
        }

        return result;
    }

    protected void setMetaInfoForConstant(IBindingContext bindingContext,
            ColumnMatch columnMatch,
            SubValue sv,
            String s,
            ConstantOpenField constantOpenField) {
        IGridTable tableBodyGrid = columnMatch.getSyntaxNode().getTableBody().getSource();
        IGrid grid = tableBodyGrid.getGrid();
        IGridRegion gridRegion = sv.getGridRegion();
        RuleRowHelper.setMetaInfoWithNodeUsageForConstantCell(grid.getCell(gridRegion.getLeft(), gridRegion.getTop()), s, constantOpenField, bindingContext);
    }

    /**
     * Prepares Nodes. Check names, operations and assigns matchers.
     * <p>
     * Special rows are ignored. That is why first n elements in return array is always null.
     *
     * @return array of nodes, elements corresponds rows
     */
    protected MatchNode[] prepareNodes(IBindingContext bindingContext,
            ColumnMatch columnMatch,
            ArgumentsHelper argumentsHelper,
            int retValuesCount) throws SyntaxNodeException {
        List<TableRow> rows = columnMatch.getRows();
        MatchNode[] nodes = new MatchNode[rows.size()];

        for (int i = getSpecialRowCount(); i < rows.size(); i++) {
            TableRow row = rows.get(i);
            SubValue nameSV = row.get(NAMES)[0];
            String varName = nameSV.getString();

            if (varName.length() == 0) {
                String msg = "Name cannot be empty!";
                throw SyntaxNodeExceptionUtils.createError(msg, nameSV.getStringValue().asSourceCodeModule());
            }

            Argument arg = argumentsHelper.getTypeByName(varName);
            if (arg == null) {
                String msg = "Failed to bind name '" + varName + "'!";
                throw SyntaxNodeExceptionUtils.createError(msg, nameSV.getStringValue().asSourceCodeModule());
            }

            SubValue operationSV = row.get(OPERATION)[0];
            String operationName = operationSV.getString();

            IMatcher matcher = MatcherFactory.getMatcher(operationName, arg.getType());
            if (matcher == null) {
                String msg = "No matcher was found for operation " + operationName + " and type " + arg.getType();
                throw SyntaxNodeExceptionUtils.createError(msg, operationSV.getStringValue().asSourceCodeModule());
            }

            MatchNode node = new MatchNode(i);
            node.setMatcher(matcher);
            node.setArgument(arg);

            parseCheckValues(bindingContext, columnMatch, row, node, retValuesCount);

            bindMetaInfo(columnMatch, varName, row.get(VALUES), node.getCheckValues());

            nodes[i] = node;
        }

        return nodes;
    }

    /**
     * Checks that tree is consistent.
     *
     * @param rootNode root of tree
     * @param rows rows to point out errors
     */
    protected void validateTree(MatchNode rootNode, List<TableRow> rows, MatchNode[] nodes) throws SyntaxNodeException {
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
}
