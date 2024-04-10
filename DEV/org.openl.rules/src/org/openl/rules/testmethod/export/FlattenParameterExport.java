package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.util.CollectionUtils;

public class FlattenParameterExport extends BaseParameterExport {

    private static final Comparator<FieldDescriptor> FIELD_ORDER = Comparator.comparing(field -> field.getField().getName());

    FlattenParameterExport(Styles styles) {
        super(styles);
    }

    @Override
    int doWrite(SXSSFSheet sheet, Cursor start, TestUnitsResults test, List<List<FieldDescriptor>> nonEmptyFields) {
        var descriptions = test.getTestSuite().getTests();
        sheet.trackAllColumnsForAutoSizing();
        int rowNum = createAndWriteRowIds(sheet, start, descriptions);
        var params = test.getTestSuite().getTest(0).getExecutionParams();
        for (int pNum = 0; pNum < params.length; pNum++) {
            final var paramN = pNum;
            rowNum = createAndWriteRowValues(sheet,
                    new Cursor(rowNum, start.getColNum()),
                    params[paramN].getName(),
                    nonEmptyFields.get(pNum),
                    descriptions,
                    description -> {
                        var execParams = ((TestDescription) description).getExecutionParams();
                        if (execParams == null || paramN >= execParams.length) {
                            return null;
                        }
                        return execParams[paramN].getValue();
                    });
        }
        for (int col = 1; col < descriptions.length + 2; col++) {
            sheet.autoSizeColumn(col);
        }
        return rowNum;
    }

    private int createAndWriteRowIds(Sheet sheet, Cursor start, TestDescription[] descriptions) {
        var colNum = start.getColNum();
        var tasks = new TreeSet<WriteTask>();
        tasks.add(new WriteTask(new Cursor(start.getRowNum(), colNum++), "ID", styles.header));

        for (var description : descriptions) {
            tasks.add(new WriteTask(new Cursor(start.getRowNum(), colNum++), description.getId(), styles.parameterValue));
        }
        performWrite(sheet, start, tasks, colNum - 1);
        return start.getRowNum() + 1;
    }

    private int createAndWriteRowValues(Sheet sheet,
                                        Cursor start,
                                        String namePrefix,
                                        List<FieldDescriptor> fields,
                                        TestDescription[] descriptions,
                                        Function<Object, Object> getFieldValueChain) {

        var rowNum = start.getRowNum();
        if (CollectionUtils.isEmpty(fields)) {
            var tasks = new TreeSet<WriteTask>();
            var colNum = start.getColNum();
            tasks.add(new WriteTask(new Cursor(start.getRowNum(), colNum++), namePrefix, styles.header));
            for (var description : descriptions) {
                var fieldValue = getFieldValueChain.apply(description);
                tasks.add(new WriteTask(new Cursor(start.getRowNum(), colNum++), fieldValue, styles.parameterValue));
            }
            performWrite(sheet, start, tasks, colNum - 1);
            return ++rowNum;
        }

        fields.sort(FIELD_ORDER);
        for (var field : fields) {
            var fieldChainName = namePrefix + "." + field.getField().getName();
            var nextValueChain = getFieldValueChain.andThen(value -> ExportUtils.fieldValue(value, field.getField()));
            var children = field.getChildren();
            if (field.isArray()) {
                int maxSize = 0;
                for (var description : descriptions) {
                    var array = nextValueChain.apply(description);
                    int size = array == null ? 0 : Array.getLength(array);
                    maxSize = Math.max(maxSize, size);
                }
                for (int i = 0; i < maxSize; i++) {
                    final int idx = i;
                    rowNum = createAndWriteRowValues(sheet,
                            new Cursor(rowNum, start.getColNum()),
                            fieldChainName + "[" + i + "]",
                            children,
                            descriptions,
                            nextValueChain.andThen(value -> {
                                if (value == null || idx >= Array.getLength(value)) {
                                    return null;
                                }
                                return Array.get(value, idx);
                            }));
                }
            } else {
                rowNum = createAndWriteRowValues(sheet,
                        new Cursor(rowNum, start.getColNum()),
                        fieldChainName,
                        children,
                        descriptions,
                        nextValueChain);
            }
        }
        return rowNum;
    }
}
