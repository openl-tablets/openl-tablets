package org.openl.rules.testmethod.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.openl.binding.impl.CastToWiderType;
import org.openl.rules.data.PrimaryKeyField;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.testmethod.*;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ClassUtils;

import java.lang.reflect.Array;
import java.util.*;

class ParameterExport extends BaseExport {
    ParameterExport(Styles styles) {
        this.styles = styles;
    }

    public void write(SXSSFSheet sheet, List<TestUnitsResults> tests) {
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
            List<List<FieldDescriptor>> nonEmptyFields = getAllNonEmptyFields(test.getTestSuite().getTests());

            // Create header
            final Cursor start = new Cursor(rowNum, colNum);
            Cursor lowestRight = writeHeaderForFields(sheet, start, test, nonEmptyFields);
            rowNum = lowestRight.getRowNum() + 1;

            rowNum = writeValuesForFields(sheet, new Cursor(rowNum, colNum), test, nonEmptyFields);

            rowNum += SPACE_BETWEEN_RESULTS;
        }
    }

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

    private Cursor writeHeaderForFields(SXSSFSheet sheet,
            Cursor start,
            TestUnitsResults test,
            List<List<FieldDescriptor>> nonEmptyFields) {
        TreeSet<WriteTask> tasks = new TreeSet<>();

        int rowNum = start.getRowNum();
        int colNum = start.getColNum();

        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), "ID", styles.header));

        TestSuite testSuite = test.getTestSuite();
        ParameterWithValueDeclaration[] params = testSuite.getTest(0).getExecutionParams();
        for (int i = 0; i < params.length; i++) {
            ParameterWithValueDeclaration param = params[i];
            boolean hasPK = isHasPK(param);

            List<FieldDescriptor> fields = nonEmptyFields.get(i);

            if (ClassUtils.isAssignable(param.getType().getInstanceClass(), Map.class)) {
                Map<?, ?> map = (Map<?, ?>) param.getValue();
                for (Object key : map.keySet()) {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum++),
                        param.getName() + "[\"" + key + "\"]:" + map.get(key).getClass().getSimpleName(),
                        styles.header));
                }
                continue;
            }

            if (fields == null || fields.isEmpty()) {
                tasks.add(new WriteTask(new Cursor(rowNum, colNum++), param.getName(), styles.header));
                continue;
            }

            String prefix = param.getName() + ".";
            if (hasPK) {
                tasks.add(new WriteTask(new Cursor(rowNum, colNum++), prefix + "_PK_", styles.header));
            }

            colNum = addHeaderTasks(tasks, new Cursor(rowNum, colNum), fields, prefix, param);

        }

        return performWrite(sheet, start, tasks, getLastColumn(test, nonEmptyFields));
    }

    private boolean isHasPK(ParameterWithValueDeclaration param) {
        return param.getKeyField() instanceof PrimaryKeyField;
    }

    private int addHeaderTasks(TreeSet<WriteTask> tasks,
            Cursor cursor,
            List<FieldDescriptor> fields,
            String prefix,
            ParameterWithValueDeclaration param) {
        int colNum = cursor.getColNum();
        int rowNum = cursor.getRowNum();

        for (FieldDescriptor fieldDescriptor : fields) {
            String fieldName = fieldDescriptor.getField().getName();

            int width = fieldDescriptor.getLeafNodeCount();

            if (fieldDescriptor.getChildren() == null) {
                if (ClassUtils.isAssignable(fieldDescriptor.getField().getType().getInstanceClass(), Map.class)) {
                    Map<?, ?> map = (Map<?, ?>) ExportUtils.fieldValue(param.getValue(), fieldDescriptor.getField());
                    for (Object key : map.keySet()) {
                        tasks.add(new WriteTask(new Cursor(rowNum, colNum++),
                            prefix + fieldName + "[\"" + key + "\"]:" + map.get(key).getClass().getSimpleName(),
                            styles.header));
                    }
                    continue;
                } else {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum), prefix + fieldName, styles.header));
                }
            } else {
                addHeaderTasks(tasks,
                    new Cursor(rowNum, colNum),
                    fieldDescriptor.getChildren(),
                    prefix + fieldName + ".",
                    param);
            }

            colNum += width;
        }

        return colNum;
    }

    private int writeValuesForFields(Sheet sheet,
            Cursor start,
            TestUnitsResults test,
            List<List<FieldDescriptor>> nonEmptyFields) {
        int rowNum = start.getRowNum();
        int colNum = FIRST_COLUMN;
        int lastColNum = getLastColumn(test, nonEmptyFields);

        TestDescription[] descriptions = test.getTestSuite().getTests();
        for (TestDescription description : descriptions) {
            TreeSet<WriteTask> tasks = new TreeSet<>();

            // ID
            int maxHeight = getMaxHeight(description, nonEmptyFields);
            tasks.add(
                new WriteTask(new Cursor(rowNum, colNum++), description.getId(), styles.parameterValue, maxHeight));

            ParameterWithValueDeclaration[] executionParams = description.getExecutionParams();
            for (int p = 0; p < executionParams.length; p++) {
                ParameterWithValueDeclaration parameter = executionParams[p];
                Object value = parameter.getValue();
                if (value instanceof Collection) {
                    value = ((Collection<?>) value).toArray();
                }

                if (value instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    for (Object val : map.values()) {
                        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), val.toString(), styles.header));
                    }
                    continue;
                }

                List<FieldDescriptor> fields = nonEmptyFields.get(p);
                if (fields == null) {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum++), value, styles.parameterValue, maxHeight));
                    continue;
                }

                // _PK_
                if (isHasPK(parameter)) {
                    IOpenField keyField = parameter.getKeyField();
                    Object id = ExportUtils.fieldValue(parameter.getValue(), keyField);

                    if (id != null && id.getClass().isArray()) {
                        int pkRow = rowNum;
                        int count = Array.getLength(id);
                        for (int i = 0; i < count; i++) {
                            int height = getRowHeight(Array.get(value, i), fields);
                            tasks.add(new WriteTask(new Cursor(pkRow, colNum),
                                Array.get(id, i),
                                styles.parameterValue,
                                height));
                            pkRow += height;
                        }
                    } else {
                        tasks.add(new WriteTask(new Cursor(rowNum, colNum), id, styles.parameterValue, maxHeight));
                    }
                    colNum++;
                }

                // Actual fields
                addValueTasks(tasks, new Cursor(rowNum, colNum), fields, value, maxHeight);
                colNum += getFieldWidth(fields);
            }

            Cursor cursor = performWrite(sheet, new Cursor(rowNum, FIRST_COLUMN), tasks, lastColNum);

            rowNum = cursor.getRowNum() + 1;
            colNum = FIRST_COLUMN;
        }

        return rowNum;
    }

    private void addValueTasks(TreeSet<WriteTask> tasks,
            Cursor cursor,
            List<FieldDescriptor> fields,
            Object value,
            int rowHeight) {
        int colNum = cursor.getColNum();
        int rowNum = cursor.getRowNum();

        if (value != null && value.getClass().isArray()) {
            int count = Array.getLength(value);
            int heightLeft = rowHeight;
            for (int i = 0; i < count; i++) {
                Object elem = Array.get(value, i);
                int height = getRowHeight(elem, fields);
                if (i < count - 1) {
                    addValueTasks(tasks, new Cursor(rowNum, colNum), fields, elem, height);
                    heightLeft -= height;
                } else {
                    addValueTasks(tasks, new Cursor(rowNum, colNum), fields, elem, heightLeft);
                }
                rowNum += height;
            }
        } else {
            for (FieldDescriptor fieldDescriptor : fields) {
                Object fieldValue = ExportUtils.fieldValue(value, fieldDescriptor.getField());
                List<FieldDescriptor> children = fieldDescriptor.getChildren();
                if (fieldValue instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) fieldValue;
                    for (Object val : map.values()) {
                        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), val.toString(), styles.header));
                    }
                    continue;
                } else if (fieldValue instanceof Collection) {
                    fieldValue = ((Collection<?>) fieldValue).toArray();
                }
                if (children == null) {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum), fieldValue, styles.parameterValue, rowHeight));
                } else {
                    addValueTasks(tasks, new Cursor(rowNum, colNum), children, fieldValue, rowHeight);
                }

                colNum += fieldDescriptor.getLeafNodeCount();
            }
        }
    }

    private int getRowHeight(Object value, List<FieldDescriptor> fields) {
        if (value == null || fields == null) {
            return 1;
        }

        if (value instanceof Collection) {
            value = ((Collection<?>) value).toArray();
        }

        if (value.getClass().isArray()) {
            int count = Array.getLength(value);
            int height = 0;
            for (int i = 0; i < count; i++) {
                height += getRowHeight(Array.get(value, i), fields);
            }
            return height == 0 ? 1 : height;
        }

        int maxSize = 1;
        for (FieldDescriptor fieldDescriptor : fields) {
            int size = fieldDescriptor.getMaxArraySize(value);
            if (size > maxSize) {
                maxSize = size;
            }
        }
        return maxSize;
    }

    private int getFieldWidth(List<FieldDescriptor> fields) {
        int colNum = 0;
        for (FieldDescriptor fieldDescriptor : fields) {
            colNum += fieldDescriptor.getLeafNodeCount();
        }

        return colNum == 0 ? 1 : colNum;

    }

    private int getMaxHeight(TestDescription description, List<List<FieldDescriptor>> nonEmptyFields) {
        int maxHeight = 1;
        ParameterWithValueDeclaration[] executionParams = description.getExecutionParams();
        for (int i = 0; i < executionParams.length; i++) {
            ParameterWithValueDeclaration param = executionParams[i];
            List<FieldDescriptor> fields = nonEmptyFields.get(i);

            int rowHeight = getRowHeight(param.getValue(), fields);
            if (rowHeight > maxHeight) {
                maxHeight = rowHeight;
            }
        }
        return maxHeight;
    }

    private List<List<FieldDescriptor>> getAllNonEmptyFields(TestDescription[] descriptions) {
        TestDescription description = descriptions[0];
        ParameterWithValueDeclaration[] executionParams = description.getExecutionParams();

        List<List<FieldDescriptor>> result = new ArrayList<>(executionParams.length);
        for (int i = 0; i < executionParams.length; i++) {
            ParameterWithValueDeclaration param = executionParams[i];
            List<Object> values = valuesForAllCases(descriptions, i);
            if (org.openl.util.ClassUtils.isAssignable(param.getType().getInstanceClass(), Collection.class)) {
                IOpenClass paramType = CastToWiderType.defineCollectionWiderType((Collection<?>) param.getValue());
                result.add(FieldDescriptor.nonEmptyFields(paramType, values));
            } else {
                result.add(FieldDescriptor.nonEmptyFields(param.getType(), values));
            }
        }

        return result;
    }

    /**
     * Due to stream nature of SXSSF, we should write row by row because of flushing if row num exceed
     * rowAccessWindowSize
     */
    private Cursor performWrite(Sheet sheet, Cursor start, TreeSet<WriteTask> tasks, int lastCellNum) {
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

    private int getLastColumn(TestUnitsResults test, List<List<FieldDescriptor>> nonEmptyFields) {
        int lastColumn = FIRST_COLUMN; // ID column
        TestSuite testSuite = test.getTestSuite();
        ParameterWithValueDeclaration[] params = testSuite.getTest(0).getExecutionParams();
        for (int i = 0; i < params.length; i++) {
            ParameterWithValueDeclaration param = params[i];
            if (isHasPK(param)) {
                lastColumn++; // _PK_ column
            }
            List<FieldDescriptor> fields = nonEmptyFields.get(i);
            if (fields == null) {
                // Simple type
                lastColumn++;
            } else {
                for (FieldDescriptor field : fields) {
                    lastColumn += field.getLeafNodeCount();
                }
            }
        }
        return lastColumn;
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

    private static final class WriteTask implements Comparable<WriteTask> {
        private final Cursor cursor;
        private final Object value;
        private final CellStyle style;
        private final int height;

        private WriteTask(Cursor cursor, Object value, CellStyle style) {
            this(cursor, value, style, 1);
        }

        private WriteTask(Cursor cursor, Object value, CellStyle style, int height) {
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
