package org.openl.extension.xmlrules.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.Condition;
import org.openl.extension.xmlrules.model.Expression;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Parameter;
import org.openl.extension.xmlrules.model.Segment;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.Table;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.ConditionImpl;
import org.openl.extension.xmlrules.model.single.ExpressionImpl;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.Range;
import org.openl.extension.xmlrules.model.single.ReturnRow;
import org.openl.extension.xmlrules.model.single.SegmentImpl;
import org.openl.extension.xmlrules.model.single.SheetHolder;
import org.openl.extension.xmlrules.model.single.TableImpl;
import org.openl.extension.xmlrules.model.single.TableRanges;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.ValueHolder;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.CellReference;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TableGridBuilder {
    private static final String FUNCTION_TABLE_SUFFIX = "_";

    private TableGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder, ExtensionModule module, Sheet sheet, Collection<OpenLMessage> messages) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (sheet.getTables() == null) {
                return;
            }

            Set<String> tablesNamesWithAttributes = new HashSet<String>();
            for (Table table : sheet.getTables()) {
                if (!table.getAttributes().isEmpty()) {
                    tablesNamesWithAttributes.add(table.getName());
                }
            }

            for (Table table : sheet.getTables()) {
                table = prepareTable(table, messages);
                Segment segment = table.getSegment();
                if (segment != null && (segment.getTotalSegments() == 1 || tablesNamesWithAttributes.contains(table.getName()))) {
                    segment = null;
                }

                String tableName = table.getName();
                boolean hasFunctionArguments = table.getParameters().size() > table.getHorizontalConditions().size() +
                        table.getVerticalConditions().size();
                if (hasFunctionArguments) {
                    if (segment != null && segment.getSegmentNumber() > 1) {
                        return;
                    }

                    createFunctionTable(gridBuilder, sheet, table);
                    tableName += FUNCTION_TABLE_SUFFIX;
                    ProjectData.getCurrentInstance().addUtilityTable(sheet, tableName);
                }
                boolean isSimpleRules = table.getHorizontalConditions().isEmpty();

                int tableWidth = getTableWidth(table, isSimpleRules);
                List<Attribute> attributes = table.getAttributes();
                if (!attributes.isEmpty()) {
                    tableWidth = Math.max(tableWidth, 3);
                }

                int tableRow = gridBuilder.getRow();

                String returnType = getReturnType(table);

                Size headerSize = writeHeader(gridBuilder, table, segment, tableName, isSimpleRules, tableWidth, returnType);

                int attributesCount = writeAttributes(gridBuilder, attributes, headerSize);

                int startColumn = gridBuilder.getStartColumn();

                writeHorizontalColumnExpressions(gridBuilder, table, segment, headerSize, startColumn);
                writeVerticalColumnHeader(gridBuilder, table, segment, isSimpleRules, tableRow, headerSize, attributesCount);

                // Don't move this expression. The order is important.
                int simpleRulesStartColumn = gridBuilder.getColumn() + table.getVerticalConditions().size();

                int maxRow = writeVerticalColumnExpressions(gridBuilder, table);
                writeReturnValues(gridBuilder, module, sheet, table, isSimpleRules, tableRow, returnType, headerSize, startColumn, simpleRulesStartColumn, messages);

                gridBuilder.setRow(Math.max(gridBuilder.getRow(), maxRow));

                gridBuilder.setStartColumn(startColumn);
                gridBuilder.nextRow();
            }
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(TableGridBuilder.class);
            log.error(e.getMessage(), e);
            messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
            gridBuilder.nextRow();
        }
    }

    private static String getReturnType(Table table) {
        String returnType = HelperFunctions.convertToOpenLType(table.getReturnType());
        if (StringUtils.isBlank(returnType)) {
            returnType = "Object";
        }
        return returnType;
    }

    private static Size writeHeader(StringGridBuilder gridBuilder,
            Table table,
            Segment segment,
            String tableName, boolean isSimpleRules, int tableWidth, String returnType) {
        Size headerSize = new Size();

        if (isTableHeaderNeeded(segment)) {
            headerSize.height++;
        }

        if (segment != null) {
            String tablePartHeader = "TablePart " + tableName +
                    (segment.isColumnSegment() ? " column " : " row ")
                    + segment.getSegmentNumber() + " of " + segment.getTotalSegments();
            gridBuilder.addCell(tablePartHeader, tableWidth).nextRow();
        }

        String tableType = isSimpleRules ? "SimpleRules" : "SimpleLookup";
        StringBuilder header = new StringBuilder();
        header.append(tableType).append(" ").append(returnType).append(" ").append(tableName).append("(");
        boolean needComma = false;
        for (Parameter parameter : table.getParameters()) {
            if (!isDimension(parameter)) {
                continue;
            }
            if (needComma) {
                header.append(", ");
            }
            String type = HelperFunctions.convertToOpenLType(parameter.getType());
            if (StringUtils.isBlank(type)) {
                type = "String";
            }
            header.append(type).append(' ').append(parameter.getName());
            needComma = true;
        }
        header.append(")");

        if (isTableHeaderNeeded(segment)) {
            gridBuilder.addCell(header.toString(), tableWidth);
            gridBuilder.nextRow();
        }
        return headerSize;
    }

    private static int writeAttributes(StringGridBuilder gridBuilder,
            List<Attribute> attributes,
            Size headerSize) {
        int attributesCount = attributes.size();
        headerSize.height += attributesCount;
        GridBuilderUtils.addAttributes(gridBuilder, attributes);
        return attributesCount;
    }

    private static void writeHorizontalColumnExpressions(StringGridBuilder gridBuilder,
            Table table,
            Segment segment,
            Size headerSize, int startColumn) {
        // HC expressions
        if (segment == null || !segment.isColumnSegment() || segment.getSegmentNumber() == 1) {
            gridBuilder.setStartColumn(startColumn + table.getVerticalConditions().size());

            for (Condition condition : table.getHorizontalConditions()) {
                for (Expression expression : condition.getExpressions()) {
                    String value = convertExpressionValue(expression);
                    gridBuilder.addCell(value, expression.getWidth());
                }
                gridBuilder.nextRow();
                headerSize.height++;
            }
            gridBuilder.setStartColumn(startColumn);
        }
    }

    private static void writeVerticalColumnHeader(StringGridBuilder gridBuilder,
            Table table,
            Segment segment,
            boolean isSimpleRules, int tableRow, Size headerSize, int attributesCount) {
        // VC header
        if (isTableHeaderNeeded(segment)) {
            if (isSimpleRules) {
                for (Parameter parameter : table.getParameters()) {
                    if (!isDimension(parameter)) {
                        continue;
                    }
                    gridBuilder.addCell(parameter.getName().toUpperCase());
                }
                gridBuilder.addCell("Return");
                gridBuilder.nextRow();
                headerSize.height++;
            } else {
                List<ParameterImpl> parameters = table.getParameters();
                for (ConditionImpl condition : table.getVerticalConditions()) {
                    Parameter parameter = parameters.get(condition.getParameterIndex());
                    gridBuilder.setCell(gridBuilder.getColumn(),
                            tableRow + 1 + attributesCount,
                            1,
                            table.getHorizontalConditions().size(),
                            parameter.getName().toUpperCase());
                    headerSize.width++;
                }
            }
        }
    }

    private static int writeVerticalColumnExpressions(StringGridBuilder gridBuilder, Table table) {
        // VC expressions
        int conditionRow = gridBuilder.getRow();
        int conditionColumn = gridBuilder.getColumn();
        int maxRow = conditionRow;
        for (Condition condition : table.getVerticalConditions()) {
            int row = conditionRow;
            for (Expression expression : condition.getExpressions()) {
                String value = convertExpressionValue(expression);
                gridBuilder.setCell(conditionColumn, row, expression.getWidth(), expression.getHeight(), value);
                row += expression.getHeight();
            }
            conditionColumn++;
            maxRow = Math.max(row, maxRow);
        }

        return maxRow;
    }

    // TODO merge startColumn and simpleRulesStartColumn
    private static void writeReturnValues(StringGridBuilder gridBuilder,
            ExtensionModule module, Sheet sheet,
            Table table,
            boolean isSimpleRules,
            int tableRow,
            String returnType,
            Size headerSize,
            int startColumn,
            int simpleRulesStartColumn,
            Collection<OpenLMessage> messages) {
        // Return values
        String workbookName = sheet.getWorkbookName();
        String sheetName = sheet.getName();
        if (isSimpleRules) {
            gridBuilder.setRow(tableRow + headerSize.height);
            gridBuilder.setStartColumn(simpleRulesStartColumn);
        } else {
            gridBuilder.setRow(tableRow + headerSize.height);
            gridBuilder.setStartColumn(startColumn + headerSize.width);
        }
        String componentType = returnType.replace("[]", "");

        for (ReturnRow returnValues : table.getReturnValues()) {
            for (Expression returnValue : returnValues.getList()) {
                try {
                    if (returnValue.getReference()) {
                        CellReference reference = CellReference.parse(workbookName, sheetName, returnValue.getValue());
                        Cell cell = GridBuilderUtils.getCell(module, reference);
                        Node node = cell.getNode();
                        if (node instanceof ValueHolder) {
                            gridBuilder.addCell(((ValueHolder) node).asString());
                        } else {
                            String cellRetrieveString = GridBuilderUtils.getCellExpression(workbookName,
                                    sheetName,
                                    reference,
                                    cell);
                            cellRetrieveString = GridBuilderUtils.wrapWithConvertFunctionIfNeeded(returnType,
                                    componentType,
                                    false,
                                    cellRetrieveString);

                            gridBuilder.addCell("= " + cellRetrieveString);
                        }
                    } else {
                        gridBuilder.addCell(returnValue.getValue());
                    }
                    if (isSimpleRules && returnValues.getList().size() > 1) {
                        Logger log = LoggerFactory.getLogger(TableGridBuilder.class);
                        log.warn("SimpleRules can't contain two-dimensional return values");
                        break;
                    }
                } catch (RuntimeException e) {
                    Logger log = LoggerFactory.getLogger(TableGridBuilder.class);
                    log.error(e.getMessage(), e);
                    messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                    gridBuilder.addCell("Error: " + e.getMessage());
                }
            }
            gridBuilder.nextRow();
        }
    }

    private static boolean isTableHeaderNeeded(Segment segment) {
        return segment == null || segment.isColumnSegment() || segment.getSegmentNumber() == 1;
    }

    private static String convertExpressionValue(Expression expression) {
        String value = expression.getValue();
        if (value == null) {
            return null;
        }

        value = value.toLowerCase();

        if ("*".equals(value)) {
            value = "";
        }
        return value;
    }

    private static void createFunctionTable(StringGridBuilder gridBuilder, Sheet sheet, Table table) {
        StringBuilder headerBuilder = new StringBuilder();
        String returnType = getReturnType(table);

        headerBuilder.append("Method ")
                .append(returnType)
                .append(' ')
                .append(table.getName())
                .append('(');
        List<ParameterImpl> parameters = table.getParameters();
        String workbookName = sheet.getWorkbookName();
        String sheetName = sheet.getName();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                headerBuilder.append(", ");
            }
            Parameter parameter = parameters.get(i);
            String type = HelperFunctions.convertToOpenLType(parameter.getType());

            if (isDimension(parameter)) {
                if (StringUtils.isBlank(type)) {
                    type = "String";
                }
                headerBuilder.append(type).append(" ").append(parameter.getName());
            } else {
                if (StringUtils.isBlank(type)) {
                    type = "Object";
                }
                CellReference cellReference = CellReference.parse(workbookName, sheetName, parameter.getName());
                headerBuilder.append(type)
                        .append(" ")
                        .append("R")
                        .append(cellReference.getRow())
                        .append("C")
                        .append(cellReference.getColumn());
            }
        }
        headerBuilder.append(')');

        List<Attribute> attributes = table.getAttributes();
        int width = attributes.isEmpty() ? 1 : 3;

        gridBuilder.addCell(headerBuilder.toString(), width).nextRow();

        GridBuilderUtils.addAttributes(gridBuilder, attributes);

        for (ParameterImpl parameter : parameters) {
            if (!isDimension(parameter)) {
                CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
                String cell;
                if (isSameSheet(reference, workbookName, sheetName)) {
                    cell = String.format("Push(%d, %d, R%sC%s);",
                            reference.getRowNumber(),
                            reference.getColumnNumber(),
                            reference.getRow(),
                            reference.getColumn());
                } else {
                    cell = String.format("Push(\"%s\", R%sC%s);",
                            reference.getStringValue(),
                            reference.getRow(),
                            reference.getColumn());
                }
                gridBuilder.addCell(cell).nextRow();
            }
        }

        StringBuilder tableInvokeString = new StringBuilder();
        tableInvokeString.append(returnType)
                .append(" result = ")
                .append(table.getName())
                .append(FUNCTION_TABLE_SUFFIX)
                .append("(");
        boolean needComma = false;
        for (ParameterImpl parameter : parameters) {
            if (isDimension(parameter)) {
                if (needComma) {
                    tableInvokeString.append(", ");
                }

                tableInvokeString.append(parameter.getName());

                needComma = true;
            }
        }
        tableInvokeString.append(");");
        gridBuilder.addCell(tableInvokeString.toString());
        gridBuilder.nextRow();

        for (ParameterImpl parameter : parameters) {
            if (!isDimension(parameter)) {
                CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
                String cell;
                if (isSameSheet(reference, workbookName, sheetName)) {
                    cell = String.format("Pop(%d, %d);", reference.getRowNumber(), reference.getColumnNumber());
                } else {
                    cell = String.format("Pop(\"%s\");", reference.getStringValue());
                }
                gridBuilder.addCell(cell).nextRow();
            }
        }

        gridBuilder.addCell("return result;").nextRow();

        gridBuilder.nextRow();
    }

    private static boolean isSameSheet(CellReference reference, String workbookName, String sheetName) {
        return reference.getWorkbook().equals(workbookName) && reference.getSheet().equals(sheetName);
    }

    private static boolean isDimension(Parameter parameter) {
        return parameter.getName().startsWith("dim");
    }

    private static Table prepareTable(Table source, Collection<OpenLMessage> messages) {
        return sortReturnCells(sortConditionsOrder(removeGapsFromReturnRows(source, messages), messages), messages);
    }

    private static Table removeGapsFromReturnRows(Table source, Collection<OpenLMessage> messages) {
        List<ConditionImpl> horizontalConditions = source.getHorizontalConditions();
        List<ConditionImpl> verticalConditions = source.getVerticalConditions();
        if (horizontalConditions.isEmpty() || verticalConditions.isEmpty()) {
            return source;
        }

        int rowCount;
        int columnCount;

        int skipRows;
        int skipColumns;

        List<ReturnRow> returnValues = source.getReturnValues();

        TableRanges tableRanges = source.getTableRanges();
        if (tableRanges == null) {
            if ((verticalConditions.size() == 0 || returnValues.size() == verticalConditions.get(0)
                    .getExpressions()
                    .size())
                    && (horizontalConditions.size() == 0 ||
                    horizontalConditions.get(0).getExpressions().size() == returnValues.get(0).getList().size())) {
                return source;
            }

            rowCount = verticalConditions.size() > 0 ? verticalConditions.get(0).getExpressions().size() : 0;
            columnCount = horizontalConditions.size() > 0 ? horizontalConditions.get(0).getExpressions().size() : 0;
            skipRows = returnValues.size() - rowCount;
            skipColumns = returnValues.get(0).getList().size() - columnCount;
        } else {
            Range verticalRange = tableRanges.getVerticalConditionsRange();
            Range horizontalRange = tableRanges.getHorizontalConditionsRange();
            Range returnValuesRange = tableRanges.getReturnValuesRange();

            int rowStart = verticalRange.getRowNumber();
            rowCount = verticalRange.getRowCount();

            int columnStart = horizontalRange.getColumnNumber();
            columnCount = horizontalRange.getColCount();

            skipRows = returnValuesRange.getRowNumber() - rowStart;
            skipColumns = returnValuesRange.getColumnNumber() - columnStart;
        }

        if (skipRows > 0 || skipColumns > 0) {
            String message = "There are gaps in the Table " + source.getName() + ". First " + skipRows + " rows and " +
                    skipColumns + " columns were skipped";
            Logger log = LoggerFactory.getLogger(TableGridBuilder.class);
            log.warn(message);
            messages.add(OpenLMessagesUtils.newWarnMessage(message));
        }

        List<ReturnRow> newReturnValues = new ArrayList<ReturnRow>();

        List<ReturnRow> subList = returnValues.subList(skipRows, skipRows + rowCount);
        for (ReturnRow returnValue : subList) {
            ReturnRow newReturnRow = new ReturnRow();
            newReturnRow.setList(returnValue.getList().subList(skipColumns, skipColumns + columnCount));
            newReturnValues.add(newReturnRow);
        }

        TableImpl table = new TableImpl();

        table.setSegment((SegmentImpl) source.getSegment());
        table.setName(source.getName());
        table.setAttributes(source.getAttributes());
        table.setReturnType(source.getReturnType());
        table.setParameters(new ArrayList<ParameterImpl>(source.getParameters()));
        table.setHorizontalConditions(new ArrayList<ConditionImpl>(horizontalConditions));
        table.setVerticalConditions(new ArrayList<ConditionImpl>(verticalConditions));
        table.setReturnValues(newReturnValues);

        return table;
    }

    private static Table sortConditionsOrder(Table source, Collection<OpenLMessage> messages) {
        Logger log = LoggerFactory.getLogger(TableGridBuilder.class);
        try {
            boolean sortedConditions = true;

            int dimensionNumber = 0;
            List<ConditionImpl> verticalConditions = source.getVerticalConditions();
            List<ConditionImpl> horizontalConditions = source.getHorizontalConditions();
            int verticalSize = verticalConditions.size();
            int horizontalSize = horizontalConditions.size();

            for (ConditionImpl condition : verticalConditions) {
                if (condition.getParameterIndex() != dimensionNumber) {
                    sortedConditions = false;
                    break;
                }
                dimensionNumber++;
            }
            for (ConditionImpl condition : horizontalConditions) {
                if (condition.getParameterIndex() != dimensionNumber) {
                    sortedConditions = false;
                    break;
                }
                dimensionNumber++;
            }

            if (sortedConditions) {
                return source;
            }

            int parametersCount = source.getParameters().size();
            List<ConditionPath> conditionPaths = new ArrayList<ConditionPath>();

            for (int parameterIndex = 0; parameterIndex < parametersCount; parameterIndex++) {
                for (int i = 0; i < verticalSize; i++) {
                    ConditionImpl condition = verticalConditions.get(i);
                    if (parameterIndex == condition.getParameterIndex()) {
                        conditionPaths.add(new ConditionPath(true, i));
                        break;
                    }
                }
                for (int i = 0; i < horizontalSize; i++) {
                    ConditionImpl condition = horizontalConditions.get(i);
                    if (parameterIndex == condition.getParameterIndex()) {
                        conditionPaths.add(new ConditionPath(false, i));
                        break;
                    }
                }
            }

            ArrayList<ConditionImpl> newVerticalConditions = new ArrayList<ConditionImpl>();
            ArrayList<ReturnRow> newReturnValues = new ArrayList<ReturnRow>();
            for (int i = 0; i < conditionPaths.size(); i++) {
                ConditionImpl condition = new ConditionImpl();
                condition.setParameterIndex(i);
                condition.setExpressions(new ArrayList<ExpressionImpl>());
                newVerticalConditions.add(condition);
            }

            if (conditionPaths.get(0).isVertical()) {
                int rows = verticalConditions.get(0).getExpressions().size();
                for (int row = 0; row < rows; row++) {
                    if (horizontalSize == 0) {
                        for (int paramIndex = 0; paramIndex < conditionPaths.size(); paramIndex++) {
                            int conditionIndex = conditionPaths.get(paramIndex).getIndex();
                            ExpressionImpl expression = verticalConditions.get(conditionIndex).getExpressions().get(row);
                            newVerticalConditions.get(paramIndex).getExpressions().add(expression);
                        }

                        fillNewReturnValues(source, newReturnValues, row, 0);
                    } else {
                        int columns = horizontalConditions.get(0).getExpressions().size();
                        for (int column = 0; column < columns; column++) {
                            fillNewVerticalConditions(source, conditionPaths, newVerticalConditions, row, column);
                            fillNewReturnValues(source, newReturnValues, row, column);
                        }
                    }
                }
            } else {
                int columns = horizontalConditions.get(0).getExpressions().size();
                for (int column = 0; column < columns; column++) {
                    if (verticalSize == 0) {
                        for (int paramIndex = 0; paramIndex < conditionPaths.size(); paramIndex++) {
                            int conditionIndex = conditionPaths.get(paramIndex).getIndex();
                            ExpressionImpl expression = horizontalConditions.get(conditionIndex).getExpressions().get(
                                    column);
                            newVerticalConditions.get(paramIndex).getExpressions().add(expression);
                        }

                        fillNewReturnValues(source, newReturnValues, 0, column);
                    } else {
                        int rows = verticalConditions.get(0).getExpressions().size();
                        for (int row = 0; row < rows; row++) {
                            fillNewVerticalConditions(source, conditionPaths, newVerticalConditions, row, column);
                            fillNewReturnValues(source, newReturnValues, row, column);
                        }
                    }
                }
            }

            TableImpl table = new TableImpl();
            table.setSegment((SegmentImpl) source.getSegment());
            table.setName(source.getName());
            table.setAttributes(source.getAttributes());
            table.setReturnType(source.getReturnType());
            table.setParameters(new ArrayList<ParameterImpl>(source.getParameters()));
            table.setHorizontalConditions(new ArrayList<ConditionImpl>());
            table.setVerticalConditions(newVerticalConditions);
            table.setReturnValues(newReturnValues);

            return table;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
            return source;
        }
    }

    private static void fillNewReturnValues(Table source, ArrayList<ReturnRow> newReturnValues, int row, int column) {
        ReturnRow newReturnRow = new ReturnRow();
        ExpressionImpl expression = source.getReturnValues().get(row).getList().get(column);
        newReturnRow.setList(Collections.singletonList(expression));

        newReturnValues.add(newReturnRow);
    }
    private static void fillNewVerticalConditions(Table source,
            List<ConditionPath> conditionPaths,
            ArrayList<ConditionImpl> newVerticalConditions, int row, int column) {
        List<ConditionImpl> verticalConditions = source.getVerticalConditions();
        List<ConditionImpl> horizontalConditions = source.getHorizontalConditions();

        for (int paramIndex = 0; paramIndex < conditionPaths.size(); paramIndex++) {
            ConditionPath conditionPath = conditionPaths.get(paramIndex);
            int conditionIndex = conditionPath.getIndex();

            ExpressionImpl expression;
            if (conditionPath.isVertical()) {
                expression = verticalConditions.get(conditionIndex).getExpressions().get(row);
            } else {
                expression = horizontalConditions.get(conditionIndex).getExpressions().get(column);
            }

            newVerticalConditions.get(paramIndex).getExpressions().add(expression);
        }
    }

    private static Table sortReturnCells(Table source, Collection<OpenLMessage> messages) {
        try {
            TableImpl table = new TableImpl();
            table.setSegment((SegmentImpl) source.getSegment());
            table.setName(source.getName());
            table.setAttributes(source.getAttributes());
            table.setReturnType(source.getReturnType());
            table.setParameters(new ArrayList<ParameterImpl>(source.getParameters()));
            table.setHorizontalConditions(new ArrayList<ConditionImpl>(source.getHorizontalConditions()));
            table.setVerticalConditions(new ArrayList<ConditionImpl>(source.getVerticalConditions()));
            table.setReturnValues(new ArrayList<ReturnRow>(source.getReturnValues()));

            final List<ConditionImpl> verticalConditions = table.getVerticalConditions();
            final List<ConditionImpl> horizontalConditions = table.getHorizontalConditions();
            List<ReturnRow> returnValues = table.getReturnValues();

            List<Integer> rowNumbers = new ArrayList<Integer>();
            List<Integer> columnNumbers = new ArrayList<Integer>();
            for (int i = 0; i < returnValues.size(); i++) {
                rowNumbers.add(i);
            }
            for (int i = 0; i < returnValues.get(0).getList().size(); i++) {
                columnNumbers.add(i);
            }

            if (verticalConditions.size() > 0) {
                Collections.sort(rowNumbers, new ConditionsComparator(verticalConditions, table));
            }

            if (horizontalConditions.size() > 0) {
                Collections.sort(columnNumbers, new ConditionsComparator(horizontalConditions, table));
            }

            for (ConditionImpl condition : verticalConditions) {
                List<ExpressionImpl> oldExpressions = condition.getExpressions();
                List<ExpressionImpl> newExpressions = new ArrayList<ExpressionImpl>();
                for (Integer rowNumber : rowNumbers) {
                    newExpressions.add(oldExpressions.get(rowNumber));
                }
                condition.setExpressions(newExpressions);
            }

            for (ConditionImpl condition : horizontalConditions) {
                List<ExpressionImpl> oldExpressions = condition.getExpressions();
                List<ExpressionImpl> newExpressions = new ArrayList<ExpressionImpl>();
                for (Integer rowNumber : columnNumbers) {
                    newExpressions.add(oldExpressions.get(rowNumber));
                }
                condition.setExpressions(newExpressions);
            }

            List<ReturnRow> newReturnValues = new ArrayList<ReturnRow>();
            for (Integer rowNumber : rowNumbers) {
                newReturnValues.add(returnValues.get(rowNumber));
            }
            returnValues = newReturnValues;

            for (ReturnRow returnRow : returnValues) {
                List<ExpressionImpl> oldExpressions = returnRow.getList();
                List<ExpressionImpl> newExpressions = new ArrayList<ExpressionImpl>();
                for (Integer rowNumber : columnNumbers) {
                    newExpressions.add(oldExpressions.get(rowNumber));
                }
                returnRow.setList(newExpressions);
            }

            table.setReturnValues(returnValues);

            return table;
        } catch (Exception e) {
            Logger log = LoggerFactory.getLogger(TableGridBuilder.class);
            log.error(e.getMessage(), e);
            messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
            return source;
        }
    }

    private static int getTableWidth(Table table, boolean isSimpleRules) {
        int tableWidth = 0;
        for (Condition condition : table.getVerticalConditions()) {
            if (condition.getExpressions().size() > 0) {
                tableWidth += condition.getExpressions().get(0).getWidth();
            }
        }

        if (table.getHorizontalConditions().size() > 0) {
            Condition condition = table.getHorizontalConditions().get(0);
            for (Expression expression : condition.getExpressions()) {
                tableWidth += expression.getWidth();
            }
        }

        if (isSimpleRules) {
            tableWidth += 1;
        }

        if (tableWidth == 0) {
            tableWidth = 1;
        }
        return tableWidth;
    }

    private static class ConditionsComparator implements Comparator<Integer> {
        private final TableImpl table;
        private final List<ConditionImpl> conditions;

        public ConditionsComparator(List<ConditionImpl> conditions, TableImpl table) {
            this.conditions = conditions;
            this.table = table;
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            for (ConditionImpl condition : conditions) {
                String v1 = condition.getExpressions().get(o1).getValue();
                String v2 = condition.getExpressions().get(o2).getValue();
                if (!v1.equals(v2)) {
                    if ("*".equals(v1)) {
                        return 1;
                    }
                    if ("*".equals(v2)) {
                        return -1;
                    }
                    return 0;
                }
            }
            throw new IllegalStateException("There are fully matched conditions in decision table '" + table.getName() + "'");
        }
    }

    private static class ConditionPath {
        private final boolean vertical;
        private final int index;

        private ConditionPath(boolean vertical, int index) {
            this.vertical = vertical;
            this.index = index;
        }

        public boolean isVertical() {
            return vertical;
        }

        public int getIndex() {
            return index;
        }
    }

    private static class Size {
        private int width = 0;
        private int height = 0;
    }
}
