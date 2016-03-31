package org.openl.extension.xmlrules.parsing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.openl.extension.xmlrules.ParseError;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.lazy.LazyCells;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.SheetHolder;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.ValueHolder;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.CellReference;
import org.openl.extension.xmlrules.utils.RulesTableReference;
import org.openl.message.OpenLMessagesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CellExpressionGridBuilder {
    private CellExpressionGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder, Sheet sheet, List<ParseError> parseErrors) {
        Logger log = LoggerFactory.getLogger(CellExpressionGridBuilder.class);
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (CollectionUtils.isEmpty(sheet.getCells())) {
                return;
            }
            final String workbookName = sheet.getWorkbookName();
            final String sheetName = sheet.getName();
            String cellsOnSheetName = new RulesTableReference(new CellReference(workbookName,
                    sheetName,
                    null,
                    null)).getTable();

            List<List<String>> conditions = new ArrayList<List<String>>();
            List<String> columnNumbers = new ArrayList<String>();
            columnNumbers.add("-");
            conditions.add(columnNumbers);
            for (LazyCells cells : sheet.getCells()) {
                for (Cell cell : cells.getCells()) {
                    if (cell.getHasArrayFormula()) {
                        // Array cells are handled differently
                        continue;
                    }

                    try {
                        // Initialize rows and columns
                        // FIXME
                        CellReference reference = CellReference.parse(workbookName, sheetName, cell.getAddress());
                        getCurrentRow(conditions, reference);
                        getCurrentColumnNumber(columnNumbers, reference);
                    } catch (RuntimeException e) {
                        log.error(e.getMessage(), e);
                        ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(),
                                gridBuilder.getColumn(),
                                cell,
                                e);
                        parseErrors.add(error);
                    }
                }
            }
            for (LazyCells cells : sheet.getCells()) {
                for (Cell cell : cells.getCells()) {
                    if (cell.getHasArrayFormula()) {
                        // Array cells are handled differently
                        continue;
                    }

                    try {
                        CellReference reference = CellReference.parse(workbookName, sheetName, cell.getAddress());
                        List<String> currentRow = getCurrentRow(conditions, reference);
                        int currentColumnNumber = getCurrentColumnNumber(columnNumbers, reference);

                        ExpressionContext expressionContext = new ExpressionContext();
                        expressionContext.setCurrentRow(reference.getRowNumber());
                        expressionContext.setCurrentColumn(reference.getColumnNumber());
                        expressionContext.setCanHandleArrayOperators(false);
                        ExpressionContext.setInstance(expressionContext);

                        while (currentRow.size() < currentColumnNumber + 1) {
                            currentRow.add(null);
                        }

                        Node node = cell.getNode();
                        String expression;
                        try {
                            if (node == null) {
                                throw new IllegalArgumentException("Cell [" + workbookName + "]" + sheetName + "!" + cell
                                        .getAddress()
                                        .toOpenLString() + " contains incorrect value. It will be skipped");
                            }
                            node.setRootNode(true);
                            if (node instanceof ValueHolder) {
                                expression = ((ValueHolder) node).asString();
                            } else {
                                expression = "= " + node.toOpenLString();
                            }
                        } catch (RuntimeException e) {
                            expression = "";
                            log.error(e.getMessage(), e);
                            ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(),
                                    currentColumnNumber,
                                    cell,
                                    e);
                            parseErrors.add(error);
                        }
                        currentRow.set(currentColumnNumber, expression);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(),
                                gridBuilder.getColumn(),
                                cell,
                                e);
                        parseErrors.add(error);
                    } finally {
                        ExpressionContext.removeInstance();
                    }
                }
            }
            addCells(gridBuilder, cellsOnSheetName, conditions);
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private static List<String> getCurrentRow(List<List<String>> conditions, CellReference reference) {
        List<String> currentRow = null;
        for (int i = 1; i < conditions.size(); i++) {
            List<String> row = conditions.get(i);
            int comparison = Integer.valueOf(row.get(0)).compareTo(Integer.valueOf(reference.getRow()));
            if (comparison == 0) {
                currentRow = row;
                break;
            } else if (comparison > 0) {
                currentRow = new ArrayList<String>();
                currentRow.add(reference.getRow());
                conditions.add(i, currentRow);
                break;
            }
        }

        if (currentRow == null) {
            currentRow = new ArrayList<String>();
            currentRow.add(reference.getRow());
            conditions.add(currentRow);
        }
        return currentRow;
    }

    private static int getCurrentColumnNumber(List<String> columnNumbers, CellReference reference) {
        int currentColumnNumber = 0;
        for (int i = 1; i < columnNumbers.size(); i++) {
            String columnNumber = columnNumbers.get(i);
            int comparison = Integer.valueOf(columnNumber.length()).compareTo(reference.getColumn().length());
            if (comparison == 0) {
                comparison = columnNumber.compareTo(reference.getColumn());
            }
            if (comparison == 0) {
                currentColumnNumber = i;
                break;
            } else if (comparison > 0) {
                columnNumbers.add(i, reference.getColumn());
                currentColumnNumber = i;
                break;
            }
        }

        if (currentColumnNumber == 0) {
            columnNumbers.add(reference.getColumn());
            currentColumnNumber = columnNumbers.size() - 1;
        }
        return currentColumnNumber;
    }

    private static void addCells(StringGridBuilder gridBuilder, String cellsOnSheetName, List<List<String>> conditions) {
        int columnsCount = conditions.get(0).size();
        gridBuilder.addCell("SimpleLookup Object " + cellsOnSheetName + "(String row, String column)", columnsCount).nextRow();

        for (List<String> row : conditions) {
            for (String cell : row) {
                gridBuilder.addCell(cell);
            }
            gridBuilder.nextRow();
        }

        gridBuilder.nextRow();
    }
}
