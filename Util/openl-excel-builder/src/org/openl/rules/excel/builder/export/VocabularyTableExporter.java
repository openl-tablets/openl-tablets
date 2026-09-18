package org.openl.rules.excel.builder.export;

import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPE_NAME;
import static org.openl.rules.excel.builder.export.DefaultValueCellWriter.writeValueToCell;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.DataTypeTableStyle;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.VocabularyModel;
import org.openl.rules.table.xls.PoiExcelHelper;

/**
 * Writes vocabulary datatypes on the Datatypes sheet.
 *
 * <p>A vocabulary is a table of one column: the header names the datatype and its base type, and every row below
 * holds one value. The tables take the styles of the datatype tables.
 */
@Slf4j
public class VocabularyTableExporter extends AbstractOpenlTableExporter<VocabularyModel> {

    @Override
    protected Cursor exportTable(VocabularyModel model, Cursor startPosition, TableStyle defaultStyle, Sheet sheet) {
        log.debug("Writing vocabulary with name {}", model.getName());
        var style = (DataTypeTableStyle) defaultStyle;
        var headerSettings = new CellRangeSettings(style.getHeaderSizeSettings().getHeight(), 0);
        addMergedHeader(sheet, startPosition, style.getHeaderStyle(), headerSettings);

        var headerText = style.getHeaderTemplate().getString().replaceAll(DATATYPE_NAME, model.getName());
        var header = new XSSFRichTextString(headerText + " <" + model.type() + ">");
        header.applyFont(style.getHeaderFont());
        PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet).setCellValue(header);

        var position = startPosition.moveDown(headerSettings.getHeight());
        var values = model.values();
        for (var i = 0; i < values.size(); i++) {
            position = position.moveDown(1);
            var cell = PoiExcelHelper.getOrCreateCell(position.getColumn(), position.getRow(), sheet);
            writeValueToCell(model.type(), values.get(i), cell);
            var rowStyle = i == values.size() - 1 ? style.getLastRowStyle() : style.getRowStyle();
            cell.setCellStyle(rowStyle.getTypeStyle());
        }
        return position;
    }

}
