package org.openl.rules.testmethod.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import org.openl.binding.impl.CastToWiderType;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
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

        int rowNum = FIRST_ROW;
        int colNum = FIRST_COLUMN;

        for (TestUnitsResults test : tests) {
            if (test.getTestSuite().getNumberOfTests() == 0) {
                continue;
            }
            Row row = sheet.createRow(rowNum);
            String testName = getTestName(test);
            createCell(row, colNum, "Parameters of " + testName, styles.parametersInfo);

            rowNum += 2; // Skip one row

            // Finding non empty fields from the test results is very expensive. Find them only once and then reuse
            // everywhere where needed.
            List<List<FieldDescriptor>> nonEmptyFields = getAllNonEmptyFields(test.getTestSuite().getTests(),
                    skipEmptyParameters);

            final Cursor start = new Cursor(rowNum, colNum);
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
        TestSuite testSuite = test.getTestSuite();
        TestSuiteMethod testSuiteMethod = testSuite.getTestSuiteMethod();
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
        TestDescription description = descriptions[0];
        ParameterWithValueDeclaration[] executionParams = description.getExecutionParams();

        List<List<FieldDescriptor>> result = new ArrayList<>(executionParams.length);
        for (int i = 0; i < executionParams.length; i++) {
            ParameterWithValueDeclaration param = executionParams[i];
            List<Object> values = valuesForAllCases(descriptions, i);
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
        List<Object> values = new ArrayList<>();
        for (TestDescription description : testDescriptions) {
            ParameterWithValueDeclaration[] executionParams = description.getExecutionParams();
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
        int lowestRowNum = start.getRowNum();
        int rightColNum = start.getColNum();
        Row row = sheet.createRow(lowestRowNum);

        for (WriteTask task : tasks) {
            Cursor cursor = task.getCursor();
            int rowNum = cursor.getRowNum();
            int colNum = cursor.getColNum();

            if (rowNum > lowestRowNum) {
                styleEmptyCells(row, start.getColNum(), lastCellNum);
                row = sheet.createRow(rowNum);
                lowestRowNum = rowNum;
            }
            if (colNum > rightColNum) {
                rightColNum = colNum;
            }

            createCell(row, colNum, task.getValue(), task.getStyle());

            int height = task.getHeight();
            if (height > 1) {
                int lastRow = rowNum + height - 1;
                CellRangeAddress region = new CellRangeAddress(rowNum, lastRow, colNum, colNum);
                // addMergedRegion() is too slow. will invoke validation later.
                row.getSheet().addMergedRegionUnsafe(region);
            }

        }

        styleEmptyCells(row, start.getColNum(), lastCellNum);

        return new Cursor(lowestRowNum, rightColNum);
    }

    private void styleEmptyCells(Row row, int firstCellNum, int lastCellNum) {
        for (int i = firstCellNum; i <= lastCellNum; i++) {
            Cell cell = row.getCell(i);
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
            Cursor cursor1 = getCursor();
            Cursor cursor2 = o.getCursor();

            int rowComparison = cursor1.getRowNum() - cursor2.getRowNum();
            return rowComparison != 0 ? rowComparison : cursor1.getColNum() - cursor2.getColNum();
        }
    }
}
