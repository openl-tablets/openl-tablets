package org.openl.rules.testmethod.export;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

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
            Function<Object, Optional<Object>> getFieldValueChain = description -> Optional.ofNullable(((TestDescription) description).getExecutionParams())
                    .filter(execParams -> paramN < execParams.length)
                    .map(execParams -> execParams[paramN].getValue());
            var maxArraySize = getMaxArraySize(descriptions, getFieldValueChain);
            var fields = nonEmptyFields.get(pNum);
            if (maxArraySize > 0) {
                // parameter is most likely an array
                for (int i = 0; i < maxArraySize; i++) {
                    final int idx = i;
                    rowNum = createAndWriteRowValues(sheet,
                            new Cursor(rowNum, start.getColNum()),
                            params[paramN].getName() + "[" + i + "]",
                            fields,
                            descriptions,
                            getFieldValueChain.andThen(opt -> opt.filter(arr -> idx < Array.getLength(arr))
                                    .map(arr -> Array.get(arr, idx))));
                }
            } else {
                rowNum = createAndWriteRowValues(sheet,
                        new Cursor(rowNum, start.getColNum()),
                        params[paramN].getName(),
                        fields,
                        descriptions,
                        getFieldValueChain);
            }
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
                                        Function<Object, Optional<Object>> getFieldValueChain) {

        var rowNum = start.getRowNum();
        if (CollectionUtils.isEmpty(fields)) {
            var tasks = new TreeSet<WriteTask>();
            var colNum = start.getColNum();
            tasks.add(new WriteTask(new Cursor(start.getRowNum(), colNum++), namePrefix, styles.header));
            for (var description : descriptions) {
                var fieldValue = getFieldValueChain.apply(description).orElse(null);
                tasks.add(new WriteTask(new Cursor(start.getRowNum(), colNum++), fieldValue, styles.parameterValue));
            }
            performWrite(sheet, start, tasks, colNum - 1);
            return ++rowNum;
        }

        fields.sort(FIELD_ORDER);
        for (var field : fields) {
            var fieldChainName = namePrefix + "." + field.getField().getName();
            Function<Object, Optional<Object>> nextValueChain = getFieldValueChain.andThen(opt -> opt.map(obj -> ExportUtils.fieldValue(obj, field.getField())));
            var children = field.getChildren();
            if (field.isArray()) {
                int maxSize = getMaxArraySize(descriptions, nextValueChain);
                for (int i = 0; i < maxSize; i++) {
                    final int idx = i;
                    rowNum = createAndWriteRowValues(sheet,
                            new Cursor(rowNum, start.getColNum()),
                            fieldChainName + "[" + i + "]",
                            children,
                            descriptions,
                            nextValueChain.andThen(opt -> opt.filter(arr -> idx < Array.getLength(arr))
                                    .map(arr -> Array.get(arr, idx))));
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

    private static int getMaxArraySize(TestDescription[] descriptions, Function<Object, Optional<Object>> getFieldValueChain) {
        return Stream.of(descriptions)
                .map(desc -> getFieldValueChain.apply(desc).orElse(null))
                .filter(Objects::nonNull)
                .filter(o -> o.getClass().isArray())
                .map(Array::getLength)
                .max(Comparator.naturalOrder())
                .orElse(0);
    }
}
