package org.openl.rules.cmatch.algorithm;

import java.text.MessageFormat;
import java.util.List;

import org.openl.binding.IBindingContext;
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
    private static final String NAMES = "names";
    public static final String OPERATION = "operation";
    public static final String VALUES = "values";

    private static final String[] REQUIRED_IDS = { NAMES, OPERATION, VALUES };

    private static final MatchAlgorithmExecutor EXECUTOR = new MatchAlgorithmExecutor();

    public void compile(IBindingContext bindingContext, ColumnMatch columnMatch) {
        checkReqColumns(columnMatch.getColumns());
        checkRows(columnMatch.getRows());

        Object[] retValues = compileReturnValues(bindingContext, columnMatch);
        columnMatch.setReturnValues(retValues);

        ArgumentsHelper argumentsHelper = new ArgumentsHelper(columnMatch.getHeader().getSignature());

        // [0] are ignored
        List<TableRow> rows = columnMatch.getRows();
        MatchNode[] nodes = prepareNodes(rows, argumentsHelper);

        MatchNode rootNode = buildNodeTree(rows, nodes);
        parseCheckValues(rows, nodes, retValues.length);

        // TODO validate CheckTree

        columnMatch.setCheckTree(rootNode);
        columnMatch.setAlgorithmExecutor(EXECUTOR);
    }

    protected String[] getRequiredColumns() {
        return REQUIRED_IDS;
    }

    /**
     * Checks that all required columns are defined.
     * 
     * @param columns
     * @see #getRequiredColumns()
     */
    void checkReqColumns(List<TableColumn> columns) {
        String[] requiredNames = getRequiredColumns();
        for (String req : requiredNames) {
            boolean exists = false;
            for (TableColumn column : columns) {
                if (req.equals(column.getId())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                throw new IllegalArgumentException("Required column '" + req + "' is absent!");
            }
        }
    }

    protected void checkRows(List<TableRow> rows) {
        for (int i = 0; i < rows.size(); i++) {
            TableRow row = rows.get(i);

            checkColumnValue(row, NAMES, false);
            checkColumnValue(row, OPERATION, false);
            checkColumnValue(row, VALUES, true);
        }
    }

    protected void checkColumnValue(TableRow row, String columnId, boolean isMultipleAlloved) {
        SubValue[] values = row.get(columnId);
        if (!isMultipleAlloved) {
            // only 1
            if (values.length != 1) {
                throw new IllegalArgumentException("Column '" + columnId + "");
            }
        }
    }

    /**
     * Compiles (parses) return values based on return type.
     * 
     * @param bindingContext
     * @param columnMatch
     * @return
     */
    protected Object[] compileReturnValues(IBindingContext bindingContext, ColumnMatch columnMatch) {
        IOpenClass returnType = columnMatch.getHeader().getType();
        IString2DataConvertor converter = String2DataConvertorFactory.getConvertor(returnType.getInstanceClass());

        TableRow retRow = columnMatch.getRows().get(0);
        SubValue[] retSubValues = retRow.get(VALUES);

        Object[] result = new Object[retSubValues.length];
        for (int i = 0; i < retSubValues.length; i++) {
            SubValue sv = retSubValues[i];
            String s = sv.getString();

            result[i] = converter.parse(s, null, bindingContext);
        }

        return result;
    }

    /**
     * Prepares Nodes. Check names, operations and assigns matchers.
     * <p>
     * 0th row is ignored. That is why 0th element in return array is always
     * null.
     * 
     * @param rows
     * @param argumentsHelper
     * @return array of nodes, elements corresponds rows
     */
    protected MatchNode[] prepareNodes(List<TableRow> rows, ArgumentsHelper argumentsHelper) {
        MatchNode[] nodes = new MatchNode[rows.size()];

        for (int i = 1; i < rows.size(); i++) {
            TableRow row = rows.get(i);
            String varName = row.get(NAMES)[0].getString();

            Argument arg = argumentsHelper.getTypeByName(varName);
            if (arg == null) {
                throw new IllegalArgumentException("Bad name '" + varName + "'!");
            }

            String operationName = row.get(OPERATION)[0].getString();

            IMatcher matcher = MatcherFactory.getMatcher(operationName, arg.getType());
            if (matcher == null) {
                throw new IllegalArgumentException("No matcher was found for operation '" + operationName
                        + "' and type " + arg.getType());
            }

            MatchNode node = new MatchNode();
            node.setMatcher(matcher);
            node.setVariableName(varName); // TODO excessive
            node.setArgument(arg);

            nodes[i] = node;
        }

        return nodes;
    }

    /**
     * Builds tree based on indentation of each row.
     * 
     * @param rows
     * @param nodes
     * @return
     */
    protected MatchNode buildNodeTree(List<TableRow> rows, MatchNode[] nodes) {
        MatchNode rootNode = new MatchNode();
        MatchNode prev = rootNode;
        int prevIndent = 0;
        for (int i = 1; i < rows.size(); i++) {
            MatchNode node = nodes[i];
            TableRow row = rows.get(i);
            int indent = row.get(NAMES)[0].getIndent();

            if (indent == 0) {
                rootNode.add(node);
            } else {
                if (indent == (prevIndent + 1)) {
                    prev.add(node);
                } else if (indent == prevIndent) {
                    prev.getParent().add(node);
                } else {
                    String msg = MessageFormat.format("Illegal indent! 0, {0} or {1} expected.", prevIndent,
                            prevIndent + 1);
                    throw new IllegalArgumentException(msg);
                }
            }

            prevIndent = indent;
            prev = node;
        }

        return rootNode;
    }

    /**
     * Parses all CheckValues for all nodes(rows). It is up to matcher (type of
     * variable in 'names') how to parse it.
     * 
     * @param rows
     * @param nodes
     * @param retValuesCount
     */
    protected void parseCheckValues(List<TableRow> rows, MatchNode[] nodes, int retValuesCount) {
        for (int i = 1; i < rows.size(); i++) {
            TableRow row = rows.get(i);
            SubValue[] inValues = row.get(VALUES);
            Object[] checkValues = new Object[retValuesCount];

            IMatcher matcher = nodes[i].getMatcher();
            for (int index = 0; index < inValues.length; index++) {
                String s = inValues[index].getString().trim();

                if (s.length() > 0) {
                    // ignore empty cells
                    Object v = matcher.fromString(s);
                    checkValues[index] = v;
                }
            }

            nodes[i].setCheckValues(checkValues);
        }
    }

    @Deprecated
    private void print(ColumnMatch columnMatch) {
        List<TableRow> rows = columnMatch.getRows();
        List<TableColumn> columns = columnMatch.getColumns();

        for (int r = 0; r < rows.size(); r++) {
            System.out.println("row #" + r);
            TableRow row = rows.get(r);

            for (TableColumn c : columns) {
                System.out.println("  column " + c.getId());
                System.out.print("   ");
                SubValue[] values = row.get(c.getId());

                for (SubValue sv : values) {
                    System.out.print(" " + sv.getIndent() + ":" + sv.getString());
                }
                System.out.println();
            }
        }
    }

}
