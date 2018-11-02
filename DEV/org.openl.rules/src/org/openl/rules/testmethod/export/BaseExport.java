package org.openl.rules.testmethod.export;

import java.util.Date;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.types.IOpenField;

public abstract class BaseExport {
    static final int FIRST_COLUMN = 1;
    static final int FIRST_ROW = 2;
    static final int SPACE_BETWEEN_RESULTS = 3;
    protected Styles styles;

    protected static void setCellComment(Cell cell, String message) {
        CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 3);

        Comment comment = cell.getSheet().createDrawingPatriarch().createCellComment(anchor);
        comment.setString(factory.createRichTextString(message));
        comment.setAuthor("OpenL");

        // Assign the comment to the cell
        cell.setCellComment(comment);
    }

    protected void autoSizeColumns(SXSSFSheet sheet) {
        short lastColumn = sheet.getRow(sheet.getLastRowNum()).getLastCellNum();

        // Skip column with Test name and ID column
        for (int i = FIRST_COLUMN + 1; i < lastColumn; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    protected Cell createCell(Row row, int cellNum, Object value, CellStyle style) {
        Cell cell = row.createCell(cellNum);

        Object simpleValue = getSimpleValue(value);
        if (simpleValue != null) {
            if (simpleValue instanceof Date) {
                style = styles.getDateStyle(row.getSheet().getWorkbook(), style);
                cell.setCellValue((Date) simpleValue);
            } else {
                cell.setCellValue(FormattersManager.format(simpleValue));
            }
        }

        cell.setCellStyle(style);

        return cell;
    }

    protected Object getSimpleValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof ParameterWithValueDeclaration) {
            ParameterWithValueDeclaration parameter = (ParameterWithValueDeclaration) value;
            Object simpleValue = parameter.getValue();

            // Return key field for complex objects
            IOpenField keyField = parameter.getKeyField();
            if (keyField != null) {
                // If key can't be found, return the object itself
                Object key = ExportUtils.fieldValue(simpleValue, keyField);
                simpleValue = key == null ? simpleValue : key;
            }

            return getSimpleValue(simpleValue);
        }

        return value;
    }
}
