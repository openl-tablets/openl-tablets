package org.openl.extension.xmlrules.parsing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.extension.xmlrules.ParseError;
import org.openl.extension.xmlrules.ProjectData;
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
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CellExpressionGridBuilder {
    private CellExpressionGridBuilder() {
    }

    public static void build(XlsSheetSourceCodeModule sheetSource, StringGridBuilder gridBuilder, Sheet sheet, List<ParseError> parseErrors, Collection<OpenLMessage> messages) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (isEmptySheet(sheet)) {
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

            initialize(columnNumbers, conditions, sheet, gridBuilder, parseErrors);

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

                        while (currentRow.size() < currentColumnNumber + 1) {
                            currentRow.add(null);
                        }

                        String expression;
                        try {
                            Node node = cell.getNode();
                            if (node instanceof ValueHolder) {
                                expression = ((ValueHolder) node).asString();
                            } else {
                                expression = "= " + GridBuilderUtils.getCellExpression(workbookName, sheetName, reference, cell);
                            }
                        } catch (RuntimeException e) {
                            expression = "";
                            Logger log = LoggerFactory.getLogger(CellExpressionGridBuilder.class);
                            log.error(e.getMessage(), e);
                            ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(),
                                    currentColumnNumber,
                                    cell,
                                    e);
                            parseErrors.add(error);
                        }
                        currentRow.set(currentColumnNumber, expression);
                    } catch (Exception e) {
                        Logger log = LoggerFactory.getLogger(CellExpressionGridBuilder.class);
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

            ProjectData projectData = ProjectData.getCurrentInstance();
            int startRow = gridBuilder.getRow();
            int startColumn = gridBuilder.getColumn();
            int endRow = startRow + conditions.size();
            int endColumn = startColumn + conditions.get(0).size() - 1;
            String uri = sheetSource.getUri() + "&range="
                    + GridBuilderUtils.toA1Row(startColumn) + (startRow + 1)
                    + "%3a" // Encoded colon
                    + GridBuilderUtils.toA1Row(endColumn) + (endRow + 1);
            projectData.addUtilityTable(sheet, cellsOnSheetName, uri);

            addCells(gridBuilder, cellsOnSheetName, conditions);
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(CellExpressionGridBuilder.class);
            log.error(e.getMessage(), e);
            messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
            gridBuilder.nextRow();
        }
    }

    private static boolean isEmptySheet(Sheet sheet) {
        if (CollectionUtils.isEmpty(sheet.getCells())) {
            return true;
        }

        for (LazyCells lazyCells : sheet.getCells()) {
            if (!lazyCells.getCells().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void initialize(List<String> columnNumbers,
            List<List<String>> conditions,
            Sheet sheet,
            StringGridBuilder gridBuilder,
            List<ParseError> parseErrors) {
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
                    CellReference reference = CellReference.parse(sheet.getWorkbookName(),
                            sheet.getName(),
                            cell.getAddress());
                    getCurrentRow(conditions, reference);
                    getCurrentColumnNumber(columnNumbers, reference);
                } catch (RuntimeException e) {
                    Logger log = LoggerFactory.getLogger(CellExpressionGridBuilder.class);
                    log.error(e.getMessage(), e);
                    ParseError error = GridBuilderUtils.createError(gridBuilder.getRow(),
                            gridBuilder.getColumn(),
                            cell,
                            e);
                    parseErrors.add(error);
                }
            }
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
