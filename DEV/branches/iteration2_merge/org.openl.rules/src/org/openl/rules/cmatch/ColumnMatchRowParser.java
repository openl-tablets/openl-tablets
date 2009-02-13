package org.openl.rules.cmatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;

@Deprecated
public class ColumnMatchRowParser {
    public static final String COLUMN_NAMES = "names";
    public static final String COLUMN_OPERATION = "operation";
    public static final String COLUMN_VALUES = "values";

    public static final String RETURN_VALUES = "Return Values";

    public String[] requiredColumns = {COLUMN_NAMES, COLUMN_OPERATION, COLUMN_VALUES};

    List<TableRow> rows;
    ColumnMatchTableParserSpecification[] specifications;

    public ColumnMatchRowParser(List<TableRow> rows, ColumnMatchTableParserSpecification[] specifications) {
        assert rows != null;
        assert rows.size() > 1;
        assert specifications != null;

        this.rows = rows;
        this.specifications = specifications;
    }

    public ColumnMatchTree parse() throws BoundError {
        ColumnMatchTree tree = new ColumnMatchTree();
        Map<Integer, ColumnMatchTreeNode> parentTree = new HashMap<Integer, ColumnMatchTreeNode>();
        int prevIndent = 0;
        tree.setReturnValues(getReturnValues(rows));
        for (TableRow row : rows) {
            checkRequiredColumns(row);
            StringValue name = getName(row);
            StringValue operation = getOperation(row);
            StringValue[] values = getValues(row);
            int indent = getIndent(row);
            ColumnMatchTableParserSpecification specification = getSpecification(operation);
            if (true) { // additional validation
                ColumnMatchTreeNode node = createTreeNode(name, operation, values, specification, null);
                if (indent == 0) {
                    tree.addNode(node);
                    if (parentTree.size() > 1) {
                        parentTree.clear();
                    }
                } else {
                    checkError(indent > (prevIndent + 1), operation,
                            "Incorrect operation indention. Expected indention is " + (prevIndent + 1));
                    checkError(parentTree.isEmpty(), operation,
                            "Incorrect operation indention. Parent operation with 0 indention is not found");
                    tree.addNode(parentTree.get(indent - 1), node);
                }
                parentTree.put(indent, node);
                prevIndent = indent;
            }
        }
        return tree;
    }

    private StringValue[] getReturnValues(List<TableRow> rows) throws BoundError {
        TableRow row = rows.get(0);
        StringValue name = getName(row);
        checkError(name.equals(RETURN_VALUES), name, "Required row with return values is not found");
        rows.remove(0);
        return getValues(row);
    }

    private boolean checkRequiredColumns(TableRow row) throws BoundError {
        for (String columnId : requiredColumns) {
            checkError(row.get(columnId) == null, null, "Required column '" + columnId + "' is not found");
        }
        return true;
    }

    private ColumnMatchTreeNode createTreeNode(StringValue name, StringValue operation, StringValue[] values,
            ColumnMatchTableParserSpecification specification, List<ColumnMatchTreeNode> children) {
        ColumnMatchTreeNode node = new ColumnMatchTreeNode();
        node.setName(name);
        node.setOperation(operation);
        node.setValues(values);
        node.setSpecification(specification);
        node.setChildren(children);
        return node;
    }

    private int getIndent(TableRow row) throws BoundError {
        SubValue name = getColumnValue(row, COLUMN_NAMES);
        return name.getIndent();
    }

    private StringValue getName(TableRow row) throws BoundError {
        SubValue name = getColumnValue(row, COLUMN_NAMES);
        return name.getStringValue();
    }

    private StringValue getOperation(TableRow row) throws BoundError {
        SubValue operation = getColumnValue(row, COLUMN_OPERATION);
        return operation.getStringValue();
    }

    private StringValue[] getValues(TableRow row) throws BoundError {
        SubValue[] rowValues = getColumnValues(row, COLUMN_VALUES);
        StringValue[] values = new StringValue[rowValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = rowValues[i].getStringValue();
        }
        return values;
    }

    private SubValue getColumnValue(TableRow row, String columnId) throws BoundError {
        SubValue value = null;
        SubValue[] values = getColumnValues(row, columnId);
        checkError(values.length == 0 || (value = values[0]) == null || value.getStringValue().isEmpty(),
                null, "Column '" + columnId + "' value must be not empty");
        checkError(values.length > 1, null, "Column '" + columnId + "' must have only one value");
        return value;
    }

    private SubValue[] getColumnValues(TableRow row, String columnId) throws BoundError {
        SubValue[] columnValues = row.get(columnId);
        checkError(columnValues == null, null, "Column '" + columnId + "' is not found");
        checkError(columnValues.length == 0, null, "Column '" + columnId + "' value must be not empty");
        return columnValues;
    }

    private void checkError(boolean errorCondition, StringValue srcValue,
            String errorMessage) throws BoundError {
        if (errorCondition) {
            throw new BoundError(errorMessage == null ? "" : errorMessage,
                    srcValue.asSourceCodeModule());
        }
    }

    private ColumnMatchTableParserSpecification getSpecification(
            StringValue operation) throws BoundError {
        for (ColumnMatchTableParserSpecification specification : specifications) {
            String specKeyword = specification.getKeyword();
            if (operation.getValue().equalsIgnoreCase(specKeyword)) {
                return specification;
            }
        }
        checkError(true, operation, "No such operation: " + operation.getValue());
        return null;
    }

}
