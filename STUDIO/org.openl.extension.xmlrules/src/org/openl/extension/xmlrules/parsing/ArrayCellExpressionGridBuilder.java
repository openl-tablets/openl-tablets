package org.openl.extension.xmlrules.parsing;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.openl.extension.xmlrules.ParseError;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.lazy.LazyCells;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.SheetHolder;
import org.openl.extension.xmlrules.model.single.node.FunctionNode;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.ValueHolder;
import org.openl.extension.xmlrules.model.single.node.expression.CellInspector;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.CellReference;
import org.openl.extension.xmlrules.utils.RulesTableReference;
import org.openl.message.OpenLMessagesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ArrayCellExpressionGridBuilder {
    private ArrayCellExpressionGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder, Sheet sheet, List<ParseError> parseErrors) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (CollectionUtils.isEmpty(sheet.getCells())) {
                return;
            }

            for (LazyCells cells : sheet.getCells()) {
                for (Cell cell : cells.getCells()) {
                    if (!cell.getHasArrayFormula()) {
                        // Non-array cells are handled differently
                        continue;
                    }

                    addArrayCells(gridBuilder, sheet, cell, parseErrors);
                }
            }

        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(ArrayCellExpressionGridBuilder.class);
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private static void addArrayCells(StringGridBuilder gridBuilder,
            Sheet sheet,
            Cell cell,
            List<ParseError> parseErrors) {
        try {
            final String workbookName = sheet.getWorkbookName();
            final String sheetName = sheet.getName();

            CellReference start = CellReference.parse(workbookName, sheetName, cell.getAddress());
            CellReference end = CellReference.parse(workbookName, sheetName, cell.getEndAddress());
            String tableName = new RulesTableReference(start, end).getTable();

            int startColumn = start.getColumnNumber();
            int startRow = start.getRowNumber();
            int endColumn = end.getColumnNumber();
            int endRow = end.getRowNumber();

            ExpressionContext expressionContext = new ExpressionContext(startRow, startColumn, endRow, endColumn);
            expressionContext.setCanHandleArrayOperators(true);
            ExpressionContext.setInstance(expressionContext);

            int columnsCount = endColumn - startColumn + 3;
            // TODO Add array operations parameters count

            Node node = cell.getNode();
            if (node == null) {
                throw new IllegalArgumentException("Cell [" + workbookName + "]" + sheetName + "!" + cell
                        .getAddress()
                        .toOpenLString() + " contains incorrect value. It will be skipped");
            }

            node.setRootNode(true);

            boolean isOutFunction = node instanceof FunctionNode && "Out".equals(((FunctionNode) node).getName());

            gridBuilder.addCell("Spreadsheet SpreadsheetResult " + tableName + "()", columnsCount).nextRow();

            gridBuilder.addCell("#");
            gridBuilder.addCell("Calculation");
            for (int i = startColumn; i <= endColumn; i++) {
                gridBuilder.addCell("C" + i + " : Object");
            }
            gridBuilder.nextRow();

            CellInspector.NodeSize nodeSize = CellInspector.inspect(cell.getNode(), true);
            int resultRows = nodeSize.getResultHeight();
            int resultColumns = nodeSize.getResultWidth();

            if (resultColumns == 1) {
                gridBuilder.addCell("Result : Object[]").addCell("= new Object[" + resultRows + "]");
            } else {
                gridBuilder.addCell("Result : Object[][]")
                        .addCell("= new Object[" + resultRows + "][" + resultColumns + "]");
            }
            gridBuilder.nextRow();

            for (int step = 0; step < resultRows; step++) {
                gridBuilder.addCell("Step" + step + " : Object");

                for (int column = startColumn; column < startColumn + resultColumns; column++) {
                    expressionContext.setCurrentRow(startRow + step);
                    expressionContext.setCurrentColumn(column);

                    String expression;
                    try {
                        if (node instanceof ValueHolder) {
                            expression = ((ValueHolder) node).asString();
                        } else {
                            String formula = isOutFunction ? ((FunctionNode) node).getArguments().get(0).toOpenLString() : node.toOpenLString();

                            if (resultColumns == 1) {
                                expression = "= $Calculation$Result[" + step + "] = " + formula;
                            } else {
                                expression = "= $Calculation$Result[" + step + "][" + (column - startColumn) + "] = " + formula;
                            }
                        }
                    } catch (RuntimeException e) {
                        expression = "";
                        Logger log = LoggerFactory.getLogger(ArrayCellExpressionGridBuilder.class);
                        log.error(e.getMessage(), e);
                        ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(), gridBuilder.getColumn(), cell, e);
                        parseErrors.add(error);
                    }
                    gridBuilder.addCell(expression);
                }
                gridBuilder.nextRow();
            }

            expressionContext.setOutArray(isOutFunction);

            // FIXME Here is simple case only
            for (int row = startRow; row <= endRow; row++) {
                gridBuilder.addCell("R" + row);
                gridBuilder.addCell(null);

                for (int column = startColumn; column <= endColumn; column++) {
                    expressionContext.setCurrentRow(row);
                    expressionContext.setCurrentColumn(column);

                    String expression;
                    try {
                        if (node instanceof ValueHolder) {
                            expression = ((ValueHolder) node).asString();
                        } else {
                            if (isOutFunction) {
                                expression = "= " + node.toOpenLString();
                            } else {
                                int rowShift = row - startRow;
                                int columnShift = column - startColumn;
                                // TODO Replace Out() function with OutArray (the function that returns Object, not String)
                                expression = "= Print(" + rowShift + ", " + columnShift + ", $Calculation$Result)";
                            }
                        }
                    } catch (RuntimeException e) {
                        expression = "";
                        Logger log = LoggerFactory.getLogger(ArrayCellExpressionGridBuilder.class);
                        log.error(e.getMessage(), e);
                        ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(),
                                gridBuilder.getColumn(),
                                cell,
                                e);
                        parseErrors.add(error);
                    }
                    gridBuilder.addCell(expression);
                }
                gridBuilder.nextRow();
            }

            gridBuilder.nextRow();
        } finally {
            ExpressionContext.removeInstance();
        }
    }

}
