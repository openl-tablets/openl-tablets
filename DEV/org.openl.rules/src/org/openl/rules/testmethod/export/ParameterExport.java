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
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.util.ClassUtils;

class ParameterExport extends BaseParameterExport {

    ParameterExport(Styles styles) {
        super(styles);
    }

    @Override
    int doWrite(SXSSFSheet sheet,
                Cursor start,
                TestUnitsResults test,
                List<List<FieldDescriptor>> nonEmptyFields,
                Boolean skipEmptyParameters) {

        var lowestRight = writeHeaderForFields(sheet, start, test, nonEmptyFields);
        var rowNum = lowestRight.getRowNum() + 1;

        return writeValuesForFields(sheet, new Cursor(rowNum, start.getColNum()), test, nonEmptyFields);
    }

    private Cursor writeHeaderForFields(SXSSFSheet sheet,
                                        Cursor start,
                                        TestUnitsResults test,
                                        List<List<FieldDescriptor>> nonEmptyFields) {
        var tasks = new TreeSet<WriteTask>();

        var rowNum = start.getRowNum();
        var colNum = start.getColNum();

        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), "ID", styles.header));

        var testSuite = test.getTestSuite();
        var params = testSuite.getTest(0).getExecutionParams();
        for (var i = 0; i < params.length; i++) {
            var param = params[i];
            var hasPK = isHasPK(param);

            var fields = nonEmptyFields.get(i);

            if (ClassUtils.isAssignable(param.getType().getInstanceClass(), Map.class)) {
                var map = (Map<?, ?>) param.getValue();
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

            var prefix = param.getName() + ".";
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
        var colNum = cursor.getColNum();
        var rowNum = cursor.getRowNum();

        for (FieldDescriptor fieldDescriptor : fields) {
            var fieldName = fieldDescriptor.getField().getName();

            var width = fieldDescriptor.getLeafNodeCount();

            if (fieldDescriptor.getChildren() == null) {
                if (ClassUtils.isAssignable(fieldDescriptor.getField().getType().getInstanceClass(), Map.class)) {
                    var map = (Map<?, ?>) ExportUtils.fieldValue(param.getValue(), fieldDescriptor.getField());
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
        var rowNum = start.getRowNum();
        var colNum = FIRST_COLUMN;
        var lastColNum = getLastColumn(test, nonEmptyFields);

        var descriptions = test.getTestSuite().getTests();
        for (TestDescription description : descriptions) {
            var tasks = new TreeSet<WriteTask>();

            // ID
            var maxHeight = getMaxHeight(description, nonEmptyFields);
            tasks.add(
                    new WriteTask(new Cursor(rowNum, colNum++), description.getId(), styles.parameterValue, maxHeight));

            var executionParams = description.getExecutionParams();
            for (var p = 0; p < executionParams.length; p++) {
                var parameter = executionParams[p];
                var value = parameter.getValue();
                if (value instanceof Collection<?> collection) {
                    value = collection.toArray();
                }

                if (value instanceof Map<?, ?> map) {
                    for (Object val : map.values()) {
                        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), val.toString(), styles.header));
                    }
                    continue;
                }

                var fields = nonEmptyFields.get(p);
                if (fields == null) {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum++), value, styles.parameterValue, maxHeight));
                    continue;
                }

                // _PK_
                if (isHasPK(parameter)) {
                    var keyField = parameter.getKeyField();
                    Object id = ExportUtils.fieldValue(parameter.getValue(), keyField);

                    if (id != null && id.getClass().isArray()) {
                        var pkRow = rowNum;
                        var count = Array.getLength(id);
                        for (var i = 0; i < count; i++) {
                            var height = getRowHeight(Array.get(value, i), fields);
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

            var cursor = performWrite(sheet, new Cursor(rowNum, FIRST_COLUMN), tasks, lastColNum);

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
        var colNum = cursor.getColNum();
        var rowNum = cursor.getRowNum();

        if (value != null && value.getClass().isArray()) {
            var count = Array.getLength(value);
            var heightLeft = rowHeight;
            for (var i = 0; i < count; i++) {
                Object elem = Array.get(value, i);
                var height = getRowHeight(elem, fields);
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
                if (fieldValue instanceof Map<?, ?> map) {
                    for (Object val : map.values()) {
                        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), val.toString(), styles.header));
                    }
                    continue;
                } else if (fieldValue instanceof Collection<?> collection) {
                    fieldValue = collection.toArray();
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

        if (value instanceof Collection<?> collection) {
            value = collection.toArray();
        }

        if (value.getClass().isArray()) {
            var count = Array.getLength(value);
            var height = 0;
            for (var i = 0; i < count; i++) {
                height += getRowHeight(Array.get(value, i), fields);
            }
            return height == 0 ? 1 : height;
        }

        var maxSize = 1;
        for (FieldDescriptor fieldDescriptor : fields) {
            var size = fieldDescriptor.getMaxArraySize(value);
            if (size > maxSize) {
                maxSize = size;
            }
        }
        return maxSize;
    }

    private int getFieldWidth(List<FieldDescriptor> fields) {
        var colNum = 0;
        for (FieldDescriptor fieldDescriptor : fields) {
            colNum += fieldDescriptor.getLeafNodeCount();
        }

        return colNum == 0 ? 1 : colNum;

    }

    private int getMaxHeight(TestDescription description, List<List<FieldDescriptor>> nonEmptyFields) {
        var maxHeight = 1;
        var executionParams = description.getExecutionParams();
        for (var i = 0; i < executionParams.length; i++) {
            var param = executionParams[i];
            var fields = nonEmptyFields.get(i);

            var rowHeight = getRowHeight(param.getValue(), fields);
            if (rowHeight > maxHeight) {
                maxHeight = rowHeight;
            }
        }
        return maxHeight;
    }

    private int getLastColumn(TestUnitsResults test, List<List<FieldDescriptor>> nonEmptyFields) {
        var lastColumn = FIRST_COLUMN; // ID column
        var testSuite = test.getTestSuite();
        var params = testSuite.getTest(0).getExecutionParams();
        for (var i = 0; i < params.length; i++) {
            var param = params[i];
            if (isHasPK(param)) {
                lastColumn++; // _PK_ column
            }
            var fields = nonEmptyFields.get(i);
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
