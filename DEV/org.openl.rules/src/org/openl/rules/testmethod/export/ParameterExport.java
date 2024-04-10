package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import org.openl.rules.data.PrimaryKeyField;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.types.IOpenField;
import org.openl.util.ClassUtils;

class ParameterExport extends BaseParameterExport {

    ParameterExport(Styles styles) {
        super(styles);
    }

    @Override
    int doWrite(SXSSFSheet sheet, Cursor start, TestUnitsResults test, List<List<FieldDescriptor>> nonEmptyFields) {
        var rowNum = start.getRowNum();
        Cursor lowestRight = writeHeaderForFields(sheet, start, test, nonEmptyFields);
        rowNum = lowestRight.getRowNum() + 1;

        return writeValuesForFields(sheet, new Cursor(rowNum, start.getColNum()), test, nonEmptyFields);
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
}
