package org.openl.rules.excel.builder.export;

import static org.openl.rules.excel.builder.export.DefaultValueCellWriter.writeDefaultValueToCell;

import java.util.Collection;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.DataTypeTableStyle;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatatypeTableExporter extends AbstractOpenlTableExporter<DatatypeModel> {

    private final Logger LOGGER = LoggerFactory.getLogger(DatatypeTableExporter.class);

    public static final String DATATYPES_SHEET = "Datatypes";

    public static final String DATATYPE_NAME = "\\{datatype.name}";

    @Override
    protected void exportTables(Collection<DatatypeModel> models, Sheet sheet) {
        Cursor endPosition = null;
        TableStyle style = getTableStyle();
        for (DatatypeModel model : models) {
            LOGGER.debug("Writing data type with name {}", model.getName());
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, startPosition, style, sheet);
        }
    }

    @Override
    protected Cursor exportTable(DatatypeModel model, Cursor startPosition, TableStyle defaultStyle, Sheet sheet) {
        DataTypeTableStyle style = (DataTypeTableStyle) defaultStyle;
        RichTextString headerTemplate = style.getHeaderTemplate();
        CellRangeSettings headerSettings = style.getHeaderSizeSettings();
        CellStyle headerStyle = style.getHeaderStyle();

        CellStyle dateStyle = style.getDateStyle();
        CellStyle dateTimeStyle = style.getDateTimeStyle();

        String dtHeaderText = headerTemplate.getString().replaceAll(DATATYPE_NAME, model.getName());
        if (StringUtils.isNotBlank(model.getParent())) {
            dtHeaderText += " extends " + model.getParent();
        }

        addMergedHeader(sheet, startPosition, headerStyle, headerSettings);

        Cell topLeftCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        RichTextString dtHeader = new XSSFRichTextString(dtHeaderText);
        dtHeader.applyFont(style.getHeaderFont());
        topLeftCell.setCellValue(dtHeader);
        startPosition = startPosition.moveDown(headerSettings.getHeight());

        Cursor endPosition = startPosition;

        Iterator<FieldModel> iterator = model.getFields().iterator();
        while (iterator.hasNext()) {
            boolean lastRow = false;
            FieldModel field = iterator.next();
            if (!iterator.hasNext()) {
                lastRow = true;
            }
            Cursor next = endPosition.moveDown(1);
            Cell typeCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            String type = field.getType();
            typeCell.setCellValue(type);
            typeCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getTypeStyle() : style.getRowStyle().getTypeStyle());
            next = next.moveRight(1);

            Cell nameCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            nameCell.setCellValue(field.getName());
            nameCell
                .setCellStyle(lastRow ? style.getLastRowStyle().getNameStyle() : style.getRowStyle().getNameStyle());
            next = next.moveRight(1);

            Cell valueCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            writeDefaultValueToCell(model, field, valueCell, dateStyle, dateTimeStyle);
            CellStyle styleAfterWrite = valueCell.getCellStyle();
            if (styleAfterWrite.getDataFormat() == 0) {
                valueCell.setCellStyle(
                    lastRow ? style.getLastRowStyle().getValueStyle() : style.getRowStyle().getValueStyle());
            }

            endPosition = next.moveLeft(2);
        }

        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    @Override
    protected String getExcelSheetName() {
        return DATATYPES_SHEET;
    }

}
