package org.openl.rules.testmethod.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import org.openl.binding.impl.CastToWiderType;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

abstract class BaseParameterExport extends BaseExport {

    BaseParameterExport(Styles styles) {
        this.styles = styles;
    }

    public void write(SXSSFSheet sheet, List<TestUnitsResults> tests, Boolean skipEmptyParameters) {
        if (tests.isEmpty()) {
            return;
        }

        var rowNum = FIRST_ROW;
        var colNum = FIRST_COLUMN;

        for (TestUnitsResults test : tests) {
            if (test.getTestSuite().getNumberOfTests() == 0) {
                continue;
            }
            var row = sheet.createRow(rowNum);
            var testName = getTestName(test);
            createCell(row, colNum, "Parameters of " + testName, styles.parametersInfo);

            rowNum += 2; // Skip one row

            // Finding non empty fields from the test results is very expensive. Find them only once and then reuse
            // everywhere where needed.
            var nonEmptyFields = getAllNonEmptyFields(test.getTestSuite().getTests(),
                    skipEmptyParameters);

            final var start = new Cursor(rowNum, colNum);
            rowNum = doWrite(sheet, start, test, nonEmptyFields, skipEmptyParameters);
            rowNum += SPACE_BETWEEN_RESULTS;
        }
    }

    abstract int doWrite(SXSSFSheet sheet,
                         Cursor start,
                         TestUnitsResults test,
                         List<List<FieldDescriptor>> nonEmptyFields,
                         Boolean skipEmptyParameters);

    private String getTestName(TestUnitsResults test) {
        var testSuite = test.getTestSuite();
        var testSuiteMethod = testSuite.getTestSuiteMethod();
        if (testSuiteMethod != null) {
            return TableSyntaxNodeUtils.getTestName(testSuiteMethod);
        } else {
            if (testSuite.getNumberOfTests() > 0) {
                return testSuite.getTest(0).getTestedMethod().getName();
            } else {
                return "Unknown";
            }
        }
    }

    private List<List<FieldDescriptor>> getAllNonEmptyFields(TestDescription[] descriptions,
                                                             Boolean skipEmptyParameters) {
        var description = descriptions[0];
        var executionParams = description.getExecutionParams();

        var result = new ArrayList<List<FieldDescriptor>>(executionParams.length);
        for (var i = 0; i < executionParams.length; i++) {
            var param = executionParams[i];
            var values = valuesForAllCases(descriptions, i);
            if (ClassUtils.isAssignable(param.getType().getInstanceClass(), Collection.class)) {
                IOpenClass paramType = CastToWiderType.defineCollectionWiderType((Collection<?>) param.getValue());
                result.add(FieldDescriptor.nonEmptyFields(paramType, values, skipEmptyParameters));
            } else {
                result.add(FieldDescriptor.nonEmptyFields(param.getType(), values, skipEmptyParameters));
            }
        }

        return result;
    }

    private List<Object> valuesForAllCases(TestDescription[] testDescriptions, int paramNum) {
        var values = new ArrayList<Object>();
        for (TestDescription description : testDescriptions) {
            var executionParams = description.getExecutionParams();
            if (executionParams.length > 0) {
                values.add(executionParams[paramNum].getValue());
            } else {
                values.add(null);
            }
        }
        return values;
    }

    /**
     * Due to stream nature of SXSSF, we should write row by row because of flushing if row num exceed
     * rowAccessWindowSize
     */
    protected Cursor performWrite(Sheet sheet, Cursor start, TreeSet<WriteTask> tasks, int lastCellNum) {
        var lowestRowNum = start.getRowNum();
        var rightColNum = start.getColNum();
        var row = sheet.createRow(lowestRowNum);

        for (WriteTask task : tasks) {
            var cursor = task.getCursor();
            var rowNum = cursor.getRowNum();
            var colNum = cursor.getColNum();

            if (rowNum > lowestRowNum) {
                styleEmptyCells(row, start.getColNum(), lastCellNum);
                row = sheet.createRow(rowNum);
                lowestRowNum = rowNum;
            }
            if (colNum > rightColNum) {
                rightColNum = colNum;
            }

            createCell(row, colNum, task.getValue(), task.getStyle());

            var height = task.getHeight();
            if (height > 1) {
                var lastRow = rowNum + height - 1;
                var region = new CellRangeAddress(rowNum, lastRow, colNum, colNum);
                // addMergedRegion() is too slow. will invoke validation later.
                row.getSheet().addMergedRegionUnsafe(region);
            }

        }

        styleEmptyCells(row, start.getColNum(), lastCellNum);

        return new Cursor(lowestRowNum, rightColNum);
    }

    private void styleEmptyCells(Row row, int firstCellNum, int lastCellNum) {
        for (var i = firstCellNum; i <= lastCellNum; i++) {
            var cell = row.getCell(i);
            if (cell == null) {
                createCell(row, i, null, styles.parameterAbsent);
            }
        }
    }

    static final class WriteTask implements Comparable<WriteTask> {
        private final Cursor cursor;
        private final Object value;
        private final CellStyle style;
        private final int height;

        WriteTask(Cursor cursor, Object value, CellStyle style) {
            this(cursor, value, style, 1);
        }

        WriteTask(Cursor cursor, Object value, CellStyle style, int height) {
            this.cursor = cursor;
            this.value = value;
            this.style = style;
            this.height = height;
        }

        public Cursor getCursor() {
            return cursor;
        }

        public Object getValue() {
            return value;
        }

        public CellStyle getStyle() {
            return style;
        }

        public int getHeight() {
            return height;
        }

        @Override
        public int compareTo(WriteTask o) {
            var cursor1 = getCursor();
            var cursor2 = o.getCursor();

            var rowComparison = cursor1.getRowNum() - cursor2.getRowNum();
            return rowComparison != 0 ? rowComparison : cursor1.getColNum() - cursor2.getColNum();
        }
    }
}
