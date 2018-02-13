package org.openl.rules.webstudio.web.test.export;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.openl.rules.data.PrimaryKeyField;
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
        ParameterWithValueDeclaration firstParam = data.get(0);
        boolean hasPK = firstParam instanceof ParameterWithValueAndPreviewDeclaration &&
                ((ParameterWithValueAndPreviewDeclaration) firstParam).getPreviewField() instanceof PrimaryKeyField;
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

        addHeaderTasks(tasks, new Cursor(rowNum, colNum), fields, "");

        return performWrite(sheet, start, tasks, getLastColumn(fields, hasPK));
    }

    private void addHeaderTasks(TreeSet<WriteTask> tasks, Cursor cursor, List<FieldDescriptor> fields, String prefix) {
        int colNum = cursor.getColNum();
        int rowNum = cursor.getRowNum();

        for (FieldDescriptor fieldDescriptor : fields) {
            String fieldName = fieldDescriptor.getField().getName();

            int width = fieldDescriptor.getLeafNodeCount();

            if (fieldDescriptor.getChildren() == null) {
                tasks.add(new WriteTask(new Cursor(rowNum, colNum), prefix + fieldName, styles.header));
            } else {
                if (fieldDescriptor.isArray()) {
                    fieldName += "[]";
                }
                addHeaderTasks(tasks, new Cursor(rowNum, colNum), fieldDescriptor.getChildren(), prefix + fieldName + ".");
            }

            colNum += width;
        }
    }

    private void writeValuesForFields(Sheet sheet,
            Cursor start,
            List<FieldDescriptor> fields,
            boolean hasPK,
            List<ParameterWithValueDeclaration> data) {
        int rowNum = start.getRowNum();
        int colNum = FIRST_COLUMN;
        int paramNum = 1;
        int lastColNum = getLastColumn(fields, hasPK);

        for (ParameterWithValueDeclaration parameter : data) {
            TreeSet<WriteTask> tasks = new TreeSet<>();
            Object value = parameter.getValue();

            // ID
            int rowHeight = getRowHeight(value, fields);
            tasks.add(new WriteTask(new Cursor(rowNum, colNum++), paramNum++, styles.parameterValue, rowHeight));

            // _PK_
            if (hasPK) {
                IOpenField previewField = ((ParameterWithValueAndPreviewDeclaration) parameter).getPreviewField();
                Object id = ExportUtils.fieldValue(parameter.getValue(), previewField);

                if (id != null && id.getClass().isArray()) {
                    int pkRow = rowNum;
                    int count = Array.getLength(id);
                    for (int i = 0; i < count; i++) {
                        int height = getRowHeight(Array.get(value, i), fields);
                        tasks.add(new WriteTask(new Cursor(pkRow, colNum), Array.get(id, i), styles.parameterValue, height));
                        pkRow += height;
                    }
                } else {
                    tasks.add(new WriteTask(new Cursor(rowNum, colNum), id, styles.parameterValue, rowHeight));
                }
                colNum++;
            }

            // Actual fields
            addValueTasks(tasks, new Cursor(rowNum, colNum), fields, value, rowHeight);
            Cursor cursor = performWrite(sheet, new Cursor(rowNum, FIRST_COLUMN), tasks, lastColNum);

            rowNum = cursor.getRowNum() + 1;
            colNum = FIRST_COLUMN;
        }
    }

    private void addValueTasks(TreeSet<WriteTask> tasks, Cursor cursor, List<FieldDescriptor> fields, Object value, int rowHeight) {
        int colNum = cursor.getColNum();
        int rowNum = cursor.getRowNum();

        if (value != null && value.getClass().isArray()) {
            int count = Array.getLength(value);
            for (int i = 0; i < count; i++) {
                Object elem = Array.get(value, i);
                int height = getRowHeight(elem, fields);
                addValueTasks(tasks, new Cursor(rowNum, colNum), fields, elem, height);
                rowNum += height;
            }
        } else {
            for (FieldDescriptor fieldDescriptor : fields) {
                Object fieldValue = ExportUtils.fieldValue(value, fieldDescriptor.getField());
                List<FieldDescriptor> children = fieldDescriptor.getChildren();

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
        if (value == null) {
            return 1;
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

    /**
     * Due to stream nature of SXSSF, we should write row by row because of flushing if row num exceed rowAccessWindowSize
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
                row.getSheet().addMergedRegion(region);
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

    private int getLastColumn(List<FieldDescriptor> fields, boolean hasPK) {
        int lastColumn = FIRST_COLUMN; // ID column
        if (hasPK) {
            lastColumn++; // _PK_ column
        }
        for (FieldDescriptor field : fields) {
            lastColumn += field.getLeafNodeCount();
        }
        return lastColumn;
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
