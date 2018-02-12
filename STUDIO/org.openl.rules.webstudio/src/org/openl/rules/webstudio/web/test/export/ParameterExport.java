package org.openl.rules.webstudio.web.test.export;

import java.lang.reflect.Array;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.webstudio.web.test.ParameterWithValueAndPreviewDeclaration;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

class ParameterExport {
    private static final int FIRST_COLUMN = 1;
    private static final int FIRST_ROW = 2;
    private final Styles styles;

    ParameterExport(Styles styles) {
        this.styles = styles;
    }

    public void write(SXSSFSheet sheet, IOpenClass type, List<ParameterWithValueDeclaration> data) {
        if (data.isEmpty()) {
            return;
        }

        List<FieldDescriptor> fields = FieldDescriptor.nonEmptyFields(type, toObjects(data));
        if (fields == null) {
            return;
        }

        int rowNum = FIRST_ROW;
        int colNum = FIRST_COLUMN;

        // Create header
        boolean hasPK = data.get(0) instanceof ParameterWithValueAndPreviewDeclaration;
        Cursor lowestRight = writeHeaderForFields(sheet, new Cursor(rowNum, colNum), fields, hasPK);
        rowNum = lowestRight.getRowNum() + 1;
        colNum = FIRST_COLUMN;

        writeValuesForFields(sheet, new Cursor(rowNum, colNum), fields, hasPK, data);
    }

    private Cursor writeHeaderForFields(Sheet sheet, Cursor start, List<FieldDescriptor> fields, boolean hasPK) {
        TreeSet<WriteTask> tasks = new TreeSet<>();

        int rowNum = start.getRowNum();
        int colNum = start.getColNum();

        tasks.add(new WriteTask(new Cursor(rowNum, colNum++), "ID", styles.header));
        if (hasPK) {
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++), "_PK_", styles.header));
        }

        addHeaderTasks(tasks, new Cursor(rowNum, colNum), fields);

        return performWrite(sheet, start, tasks);
    }

    private void addHeaderTasks(TreeSet<WriteTask> tasks, Cursor cursor, List<FieldDescriptor> fields) {
        int colNum = cursor.getColNum();
        int rowNum = cursor.getRowNum();

        for (FieldDescriptor fieldDescriptor : fields) {
            String fieldName = fieldDescriptor.getField().getName();

            int width = fieldDescriptor.getWidth();

            if (fieldDescriptor.getChildren() == null) {
                tasks.add(new WriteTask(new Cursor(rowNum, colNum), fieldName, styles.header));
            } else {
                tasks.add(new WriteTask(new Cursor(rowNum, colNum), fieldName, styles.header, width));
                addHeaderTasks(tasks, new Cursor(rowNum + 1, colNum), fieldDescriptor.getChildren());
            }

            colNum += width;
        }
    }

    private void writeValuesForFields(Sheet sheet,
            Cursor start,
            List<FieldDescriptor> fields,
            boolean hasPK,
            List<ParameterWithValueDeclaration> data) {
        TreeSet<WriteTask> tasks = new TreeSet<>();

        int rowNum = start.getRowNum();
        int colNum = FIRST_COLUMN;
        int paramNum = 1;
        for (ParameterWithValueDeclaration parameter : data) {
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++), paramNum++, styles.resultOther));

            if (hasPK) {
                IOpenField previewField = ((ParameterWithValueAndPreviewDeclaration) parameter).getPreviewField();
                Object id = ExportUtils.fieldValue(parameter.getValue(), previewField);

                if (id != null && id.getClass().isArray()) {
                    int count = Array.getLength(id);
                    for (int i = 0; i < count; i++) {
                        tasks.add(new WriteTask(new Cursor(rowNum + i, colNum), Array.get(id, i), styles.resultOther));
                    }
                } else {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum), id, styles.resultOther));
                }
                colNum++;
            }

            addValueTasks(tasks, new Cursor(rowNum, colNum), fields, parameter.getValue());
            Cursor cursor = performWrite(sheet, new Cursor(rowNum, FIRST_COLUMN), tasks);

            rowNum = cursor.getRowNum() + 1;
            colNum = FIRST_COLUMN;
        }
    }

    private void addValueTasks(TreeSet<WriteTask> tasks, Cursor cursor, List<FieldDescriptor> fields, Object value) {
        int colNum = cursor.getColNum();
        int rowNum = cursor.getRowNum();

        if (value != null && value.getClass().isArray()) {
            int count = Array.getLength(value);
            for (int i = 0; i < count; i++) {
                addValueTasks(tasks, new Cursor(rowNum + i, colNum), fields, Array.get(value, i));
            }
        } else {
            for (FieldDescriptor fieldDescriptor : fields) {
                Object fieldValue = ExportUtils.fieldValue(value, fieldDescriptor.getField());
                List<FieldDescriptor> children = fieldDescriptor.getChildren();

                if (children == null) {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum), fieldValue, styles.resultOther));
                } else {
                    // Keep first row empty, begin writing on next row to show that this is a child field.
                    addValueTasks(tasks, new Cursor(rowNum + 1, colNum), children, fieldValue);
                }

                colNum += fieldDescriptor.getWidth();
            }
        }
    }

    /**
     * Due to stream nature of SXSSF, we should write row by row because of flushing if row num exceed rowAccessWindowSize
     */
    private Cursor performWrite(Sheet sheet, Cursor start, TreeSet<WriteTask> tasks) {
        int lowestRowNum = start.getRowNum();
        int rightColNum = start.getColNum();
        Row row = sheet.createRow(lowestRowNum);

        for (WriteTask task : tasks) {
            Cursor cursor = task.getCursor();
            int rowNum = cursor.getRowNum();
            int colNum = cursor.getColNum();

            if (rowNum > lowestRowNum) {
                row = row.getSheet().createRow(rowNum);
                lowestRowNum = rowNum;
            }
            if (colNum > rightColNum) {
                rightColNum = colNum;
            }

            createCell(row, colNum, task.getValue(), task.getStyle());

            int width = task.getWidth();
            if (width > 1) {
                row.getSheet().addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, colNum + width - 1));
            }

        }

        return new Cursor(lowestRowNum, rightColNum);
    }

    private List<Object> toObjects(List<ParameterWithValueDeclaration> data) {
        List<Object> values = new ArrayList<>();
        for (ParameterWithValueDeclaration param : data) {
            values.add(param.getValue());
        }
        return values;
    }


    private void createCell(Row row, int cellNum, Object value, CellStyle style) {
        Cell cell = row.createCell(cellNum);

        if (value != null) {
            if (value instanceof Date) {
                style = styles.getDateStyle(row.getSheet().getWorkbook(), style);
                cell.setCellValue((Date) value);
            } else {
                cell.setCellValue(FormattersManager.format(value));
            }
        }

        cell.setCellStyle(style);
    }

    private static class WriteTask implements Comparable<WriteTask> {
        private final Cursor cursor;
        private final Object value;
        private final CellStyle style;
        private final int width;

        private WriteTask(Cursor cursor, Object value, CellStyle style) {
            this(cursor, value, style, 1);
        }

        private WriteTask(Cursor cursor, Object value, CellStyle style, int width) {
            this.cursor = cursor;
            this.value = value;
            this.style = style;
            this.width = width;
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

        public int getWidth() {
            return width;
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
