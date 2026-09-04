package org.openl.rules.excel.builder.export;

import static org.openl.rules.excel.builder.export.DefaultValueCellWriter.writeDefaultValueToCell;

import java.util.Collection;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import org.openl.rules.excel.builder.template.DataTypeTableStyle;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.util.StringUtils;

@Slf4j
public class DatatypeTableExporter extends AbstractOpenlTableExporter<DatatypeModel> {


    public static final String DATATYPES_SHEET = "Datatypes";

    public static final String DATATYPE_NAME = "\\{datatype.name}";

    @Override
    protected void exportTables(Collection<DatatypeModel> models, Sheet sheet) {
        Cursor endPosition = null;
        var style = getTableStyle();
        for (DatatypeModel model : models) {
            log.debug("Writing data type with name {}", model.getName());
            var startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, startPosition, style, sheet);
        }
    }

    @Override
    protected Cursor exportTable(DatatypeModel model, Cursor startPosition, TableStyle defaultStyle, Sheet sheet) {
        var style = (DataTypeTableStyle) defaultStyle;
        var headerTemplate = style.getHeaderTemplate();
        var headerSettings = style.getHeaderSizeSettings();
        var headerStyle = style.getHeaderStyle();

        var dateStyle = style.getDateStyle();
        var dateTimeStyle = style.getDateTimeStyle();

        var dtHeaderText = headerTemplate.getString().replaceAll(DATATYPE_NAME, model.getName());
        if (StringUtils.isNotBlank(model.getParent())) {
            dtHeaderText += " extends " + model.getParent();
        }

        addMergedHeader(sheet, startPosition, headerStyle, headerSettings);

        Cell topLeftCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        var dtHeader = new XSSFRichTextString(dtHeaderText);
        dtHeader.applyFont(style.getHeaderFont());
        topLeftCell.setCellValue(dtHeader);
        startPosition = startPosition.moveDown(headerSettings.getHeight());

        var endPosition = startPosition;

        Iterator<FieldModel> iterator = model.getFields().iterator();
        while (iterator.hasNext()) {
            var lastRow = false;
            var field = iterator.next();
            if (!iterator.hasNext()) {
                lastRow = true;
            }
            var next = endPosition.moveDown(1);
            Cell typeCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
            var type = field.getType();
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
            var styleAfterWrite = valueCell.getCellStyle();
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
