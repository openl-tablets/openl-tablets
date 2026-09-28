package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.jspecify.annotations.Nullable;

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

            var keys = mapKeys(testSuite.getTests(), i);
            if (keys != null) {
                colNum = addMapHeaderTasks(tasks, rowNum, colNum, param.getName(), keys,
                        testSuite.getTests(), i);
            } else if (fields == null || fields.isEmpty()) {
                tasks.add(new WriteTask(new Cursor(rowNum, colNum++), param.getName(), styles.header));
            } else {
                var prefix = param.getName() + ".";
                if (hasPK) {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum++), prefix + "_PK_", styles.header));
                }

                colNum = addHeaderTasks(tasks, new Cursor(rowNum, colNum), fields, prefix, param);
            }

        }

        return performWrite(sheet, start, tasks, getLastColumn(test, nonEmptyFields));
    }

    /**
     * A column for every key the map of a parameter carries in any of the cases, named by that key and by what
     * is held under it.
     *
     * <p>A key nothing is held under anywhere is named by the key alone: there is no value to take a type from.
     */
    private int addMapHeaderTasks(TreeSet<WriteTask> tasks,
                                  int rowNum,
                                  int colNum,
                                  String name,
                                  List<Object> keys,
                                  TestDescription[] cases,
                                  int paramNum) {
        for (Object key : keys) {
            var held = heldUnder(cases, paramNum, key);
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++),
                    name + "[\"" + key + "\"]" + (held == null ? "" : ":" + held.getClass().getSimpleName()),
                    styles.header));
        }
        return colNum;
    }

    /**
     * The keys the map of the given parameter carries across all the cases, in the order they first appear, or
     * {@code null} where the parameter is not a map, or is one that no case put anything in.
     *
     * <p>The columns of a map are laid out once for the whole table. Taken from the first case alone, they would
     * be too few for a case that holds more, and that case's values would be written over the columns of the
     * parameter standing after it.
     *
     * <p>A map no case put anything in — including one nothing was given for at all — is written the way any
     * other value without fields is, under the name of the parameter alone. Laid out as a map it would take no
     * column at all, and the parameter would go unmentioned.
     */
    private static @Nullable List<Object> mapKeys(TestDescription[] cases, int paramNum) {
        if (!ClassUtils.isAssignable(cases[0].getExecutionParams()[paramNum].getType().getInstanceClass(), Map.class)) {
            return null;
        }
        var keys = new LinkedHashSet<>();
        for (TestDescription one : cases) {
            if (mapOf(one, paramNum) instanceof Map<?, ?> map) {
                keys.addAll(map.keySet());
            }
        }
        return keys.isEmpty() ? null : List.copyOf(keys);
    }

    /**
     * The columns of a map held in a field, taken from the object at hand.
     *
     * <p>Unlike the map of a parameter, a map inside an object is laid out from the case being written rather
     * than from all of them, which is how this export has always drawn it: reaching a field means walking the
     * fields of every case, and the sheet has no place to keep what that walk found. Two cases whose maps carry
     * different keys are drawn one under the other all the same, which is the older shortcoming this export
     * carries; the map of a parameter no longer does.
     */
    private int addFieldMapHeaderTasks(TreeSet<WriteTask> tasks, int rowNum, int colNum, String name, Map<?, ?> map) {
        for (var entry : map.entrySet()) {
            var held = entry.getValue();
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++),
                    name + "[\"" + entry.getKey() + "\"]" + (held == null ? "" : ":" + held.getClass().getSimpleName()),
                    styles.header));
        }
        return colNum;
    }

    /** @see #addFieldMapHeaderTasks */
    private int addFieldMapValueTasks(TreeSet<WriteTask> tasks, int rowNum, int colNum, Map<?, ?> map) {
        for (Object val : map.values()) {
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++), val == null ? null : val.toString(), styles.header));
        }
        return colNum;
    }

    /** What is held under the key by the first case that holds anything under it. */
    private static @Nullable Object heldUnder(TestDescription[] cases, int paramNum, Object key) {
        for (TestDescription one : cases) {
            if (mapOf(one, paramNum) instanceof Map<?, ?> map && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }

    private static @Nullable Object mapOf(TestDescription one, int paramNum) {
        var params = one.getExecutionParams();
        return paramNum < params.length ? params[paramNum].getValue() : null;
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
                    if (map != null) {
                        colNum = addFieldMapHeaderTasks(tasks, rowNum, colNum, prefix + fieldName, map);
                        continue;
                    }
                }
                tasks.add(new WriteTask(new Cursor(rowNum, colNum), prefix + fieldName, styles.header));
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

                var keys = mapKeys(descriptions, p);
                if (keys != null) {
                    // Under the columns the whole table was laid out with: a key this case does not carry
                    // leaves its column empty rather than moving the values beside it.
                    colNum = addMapValueTasks(tasks, rowNum, colNum, (Map<?, ?>) value, keys);
                    continue;
                }

                var fields = nonEmptyFields.get(p);
                colNum = addParameterValueTasks(tasks, rowNum, colNum, parameter, value, fields, maxHeight);
            }

            var cursor = performWrite(sheet, new Cursor(rowNum, FIRST_COLUMN), tasks, lastColNum);

            rowNum = cursor.getRowNum() + 1;
            colNum = FIRST_COLUMN;
        }

        return rowNum;
    }

    private int addMapValueTasks(TreeSet<WriteTask> tasks,
                                 int rowNum,
                                 int colNum,
                                 @Nullable Map<?, ?> map,
                                 List<Object> keys) {
        for (Object key : keys) {
            var val = map == null ? null : map.get(key);
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++), val == null ? null : val.toString(), styles.header));
        }
        return colNum;
    }

    /**
     * Adds the tasks that write the value of a parameter: the primary key and the fields of a parameter with fields,
     * or the value itself otherwise.
     *
     * @return the column to write the next parameter to
     */
    private int addParameterValueTasks(TreeSet<WriteTask> tasks,
                                       int rowNum,
                                       int colNum,
                                       ParameterWithValueDeclaration parameter,
                                       Object value,
                                       List<FieldDescriptor> fields,
                                       int maxHeight) {
        if (fields == null) {
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++), value, styles.parameterValue, maxHeight));
        } else {
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
        return colNum;
    }

    private void addValueTasks(TreeSet<WriteTask> tasks,
                               Cursor cursor,
                               List<FieldDescriptor> fields,
                               Object value,
                               int rowHeight) {
        var colNum = cursor.getColNum();
        var rowNum = cursor.getRowNum();

        if (value != null && value.getClass().isArray()) {
            addArrayValueTasks(tasks, rowNum, colNum, fields, value, rowHeight);
        } else {
            for (FieldDescriptor fieldDescriptor : fields) {
                Object fieldValue = ExportUtils.fieldValue(value, fieldDescriptor.getField());
                List<FieldDescriptor> children = fieldDescriptor.getChildren();
                if (fieldValue instanceof Map<?, ?> map) {
                    colNum = addFieldMapValueTasks(tasks, rowNum, colNum, map);
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

    /**
     * Adds the tasks that write the elements of an array one under another. The last element takes the rest of the
     * row height.
     */
    private void addArrayValueTasks(TreeSet<WriteTask> tasks,
                                    int rowNum,
                                    int colNum,
                                    List<FieldDescriptor> fields,
                                    Object value,
                                    int rowHeight) {
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
            var keys = mapKeys(testSuite.getTests(), i);
            var fields = nonEmptyFields.get(i);
            if (keys != null) {
                // A column per key of the map, as the header lays them out.
                lastColumn += keys.size();
            } else if (fields == null) {
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
