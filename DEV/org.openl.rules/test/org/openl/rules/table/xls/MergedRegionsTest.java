package org.openl.rules.table.xls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.types.meta.EmptyMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriterImpl;
import org.openl.rules.table.GridTool;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * Tests correctness of resizing and moving merged regions during removing/inserting of columns and rows.
 * <p>
 * Test format: see in "MergedRegions.xls"
 *
 * @author PUdalau
 */
class MergedRegionsTest {

    /**
     * Description of test case: region to do action(insert or remove), region with expected result, first row/column to
     * do action, number of rows/columns
     */
    private static class TestDesctiption {
        private static final String testDescriptionFormat = "test=.+&result=.+&original=.+&from=\\d+&count=\\d+";
        private static final Pattern testDescriptionPattern = Pattern.compile(testDescriptionFormat);

        private IGridRegion testRegion;
        private IGridRegion expectedResultRegion;
        private IGridRegion originalTableRegion;
        private int from;
        private int count;

        public IGridRegion getTestRegion() {
            return testRegion;
        }

        public IGridRegion getExpectedResultRegion() {
            return expectedResultRegion;
        }

        public IGridRegion getOriginalTableRegion() {
            return originalTableRegion;
        }

        public int getFrom() {
            return from;
        }

        public int getCount() {
            return count;
        }

        private static boolean isTestDescriptionString(String descriptionString) {

            return descriptionString != null && testDescriptionPattern.matcher(descriptionString).matches();
        }

        public static TestDesctiption parse(String descriptionString) {
            var test = new TestDesctiption();
            var tokenizer = new StringTokenizer(descriptionString, "&");
            while (tokenizer.hasMoreElements()) {
                var param = (String) tokenizer.nextElement();
                var index = param.indexOf('=');
                var key = param.substring(0, index);
                var value = param.substring(index + 1);
                if ("test".equals(key)) {
                    test.testRegion = new XlsGridRegion(CellRangeAddress.valueOf(value));
                } else if ("result".equals(key)) {
                    test.expectedResultRegion = new XlsGridRegion(CellRangeAddress.valueOf(value));
                } else if ("original".equals(key)) {
                    test.originalTableRegion = new XlsGridRegion(CellRangeAddress.valueOf(value));
                } else if ("from".equals(key)) {
                    test.from = Integer.parseInt(value);
                } else if ("count".equals(key)) {
                    test.count = Integer.parseInt(value);
                }
            }
            return test;
        }
    }

    /**
     * Service exception.
     * <p>
     * Signals that difference between result cell and expected cell has been detected.
     */
    private static class DifferentCellsException extends Exception {
        private static final long serialVersionUID = 1L;
        private final ICell resultCell;
        private final ICell expectedCell;

        public ICell getResultCell() {
            return resultCell;
        }

        public ICell getExpectedCell() {
            return expectedCell;
        }

        public DifferentCellsException(ICell resultCell, ICell expectedCell) {
            this.resultCell = resultCell;
            this.expectedCell = expectedCell;
        }

    }

    private static final String __src = "test/rules/MergedRegions.xls";

    private List<TestDesctiption> findAllTests(IWritableGrid grid) {
        var result = new ArrayList<TestDesctiption>();
        for (var row = 0; row <= grid.getMaxRowIndex(); row++) {
            for (var column = 0; column <= grid.getMaxColumnIndex(row); column++) {
                var cell = grid.getCell(column, row);
                if (cell != null) {
                    var descriptionString = cell.getStringValue();
                    if (TestDesctiption.isTestDescriptionString(descriptionString)) {
                        result.add(TestDesctiption.parse(descriptionString));
                    }
                }
            }
        }
        return result;
    }

    private void compareTablesByCell(IGridRegion testRegion,
                                     IGridRegion expectedRegion,
                                     XlsSheetGridModel grid) throws DifferentCellsException {
        var height = Math.max(IGridRegion.Tool.height(testRegion), IGridRegion.Tool.height(expectedRegion));
        var width = Math.max(IGridRegion.Tool.width(testRegion), IGridRegion.Tool.width(expectedRegion));
        for (var row = 0; row <= height; row++) {
            for (var column = 0; column <= width; column++) {
                var resultCell = (XlsCell) grid.getCell(testRegion.getLeft() + column, testRegion.getTop() + row);
                var expectedCell = (XlsCell) grid.getCell(expectedRegion.getLeft() + column,
                        expectedRegion.getTop() + row);
                Cell resultXLSCell = PoiExcelHelper.getOrCreateCell(testRegion.getLeft() + column,
                        testRegion.getTop() + row,
                        grid.getSheetSource().getSheet());
                Cell expectedXLSCell = PoiExcelHelper.getOrCreateCell(expectedRegion.getLeft() + column,
                        expectedRegion.getTop() + row,
                        grid.getSheetSource().getSheet());
                if (resultCell != expectedCell && !Objects.equals(resultCell.getStringValue(),
                        expectedCell.getStringValue())) {
                    // non top left cells of merged regions will be skipped in
                    // comparing by POI due to the second check
                    // TODO:remove the second check when the bug with non empty
                    // cells in merged regions will be resolved
                    if (!isEqualCells(resultCell, expectedCell, grid) || !isEqualCellsInPOI(resultXLSCell,
                            expectedXLSCell)) {
                        throw new DifferentCellsException(resultCell, expectedCell);
                    }
                }
            }
        }
        assertTrue(true);
    }

    private boolean isEqualCells(ICell first, ICell second, XlsSheetGridModel grid) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        if (grid.isPartOfTheMergedRegion(first.getAbsoluteColumn(), first.getAbsoluteRow()) != grid
                .isPartOfTheMergedRegion(second.getAbsoluteColumn(), second.getAbsoluteRow())) {
            return false;
        }
        var firstValue = first.getStringValue();
        var secondValue = second.getStringValue();
        if (firstValue != null) {
            return firstValue.equals(secondValue);
        } else {
            return secondValue == null;
        }
    }

    private boolean isEqualCellsInPOI(Cell first, Cell second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        if (first.getCellType() != second.getCellType()) {
            return false;
        }
        var firstValue = extractCellValue(first);
        var secondValue = extractCellValue(second);
        if (firstValue != null) {
            return firstValue.equals(secondValue);
        } else {
            return secondValue == null;
        }
    }

    private Object extractCellValue(Cell cell) {
        var type = cell.getCellType();
        return switch (type) {
            case BLANK -> null;
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> cell.getStringCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> "unknown type: " + cell.getCellType();
        };
    }

    private void testActions(XlsSheetGridModel grid,
                             IGridTable table,
                             TestDesctiption test,
                             IUndoableGridTableAction removeRowsActions) {
        try {
            removeRowsActions.doAction(table);
            compareTablesByCell(test.getTestRegion(), test.getExpectedResultRegion(), grid);
            removeRowsActions.undoAction(table);
            compareTablesByCell(test.getTestRegion(), test.getOriginalTableRegion(), grid);
        } catch (DifferentCellsException e) {
            fail("Different cells:\n" + e.getResultCell().getUri() + "\n and \n" + e.getExpectedCell().getUri());
        }
    }

    @Test
    void testDeleteRows() {
        var workbook = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(__src));
        var sheet = new XlsSheetSourceCodeModule(
                new SimpleSheetLoader(workbook.getWorkbook().getSheet("DeleteRows")),
                workbook);
        var grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(8, tests.size());
        var table = grid.getTables()[0];
        var metaInfoWriter = new MetaInfoWriterImpl(EmptyMetaInfoReader.getInstance(), table);
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction removeRowsAction = GridTool
                    .removeRows(test.getCount(), test.getFrom(), test.getTestRegion(), table.getGrid(), metaInfoWriter);
            testActions(grid, table, test, removeRowsAction);
        }
    }

    @Test
    void testInsertRows() {
        var workbook = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(__src));
        var sheet = new XlsSheetSourceCodeModule(
                new SimpleSheetLoader(workbook.getWorkbook().getSheet("InsertRows")),
                workbook);
        var grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(7, tests.size());
        var table = grid.getTables()[0];
        var metaInfoWriter = new MetaInfoWriterImpl(EmptyMetaInfoReader.getInstance(), table);
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction insertRowsAction = GridTool
                    .insertRows(test.getCount(), test.getFrom(), test.getTestRegion(), table.getGrid(), metaInfoWriter);
            testActions(grid, table, test, insertRowsAction);
        }
    }

    @Test
    void testDeleteColumns() {
        var workbook = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(__src));
        var sheet = new XlsSheetSourceCodeModule(
                new SimpleSheetLoader(workbook.getWorkbook().getSheet("DeleteColumns")),
                workbook);
        var grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(6, tests.size());
        var table = grid.getTables()[0];
        var metaInfoWriter = new MetaInfoWriterImpl(EmptyMetaInfoReader.getInstance(), table);
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction removeColumnsAction = GridTool
                    .removeColumns(test.getCount(), test.getFrom(), test.getTestRegion(), table.getGrid(), metaInfoWriter);
            testActions(grid, table, test, removeColumnsAction);
        }
    }

    @Test
    void testInsertColumn() {
        var workbook = new XlsWorkbookSourceCodeModule(new URLSourceCodeModule(__src));
        var sheet = new XlsSheetSourceCodeModule(
                new SimpleSheetLoader(workbook.getWorkbook().getSheet("InsertColumns")),
                workbook);
        var grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(7, tests.size());
        var table = grid.getTables()[0];
        var metaInfoWriter = new MetaInfoWriterImpl(EmptyMetaInfoReader.getInstance(), table);
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction insertColumnsAction = GridTool
                    .insertColumns(test.getCount(), test.getFrom(), test.getTestRegion(), table.getGrid(), metaInfoWriter);
            testActions(grid, table, test, insertColumnsAction);
        }
    }
}
