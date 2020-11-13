package org.openl.rules.excel.builder.export;

import static org.openl.rules.excel.builder.export.DefaultValueCellWriter.writeDefaultValueToCell;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.DataTableStyle;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.FieldModel;
import org.openl.rules.model.scaffolding.data.DataModel;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTableExporter extends AbstractOpenlTableExporter<DataModel> {

    public static final String DATA_SHEET = "Data Table";
    public static final String DATA_TYPE_NAME = "{returnType}";
    public static final String DATA_TABLE_NAME = "{table.name}";
    public static final int ROWS_COUNT = 3;

    private final Logger LOGGER = LoggerFactory.getLogger(DataTableExporter.class);

    @Override
    protected void exportTables(Collection<DataModel> models, Sheet sheet) {
        Cursor endPosition = null;
        TableStyle style = getTableStyle();
        for (DataModel model : models) {
            LOGGER.debug("exporting data table with name {}", model.getName());
            Cursor startPosition = nextFreePosition(endPosition);
            endPosition = exportTable(model, startPosition, style, sheet);
        }
    }

    @Override
    protected Cursor exportTable(DataModel model, Cursor startPosition, TableStyle defaultStyle, Sheet sheet) {
        DataTableStyle style = (DataTableStyle) defaultStyle;

        RichTextString dataTableHeaderTemplate = style.getHeaderTemplate();
        CellRangeSettings headerSettings = style.getHeaderSizeSettings();

        CellStyle subheaderStyle = style.getSubheaderStyle();

        Font tableNameFont = style.getTableNameFont();
        Font typeFont = style.getTypeFont();

        CellStyle headerStyle = style.getHeaderStyle();
        CellStyle dateStyle = style.getDateStyle();
        CellStyle dateTimeStyle = style.getDateTimeStyle();

        String type = model.getType();
        String dataHeaderText = dataTableHeaderTemplate.getString().replace(DATA_TYPE_NAME, type);
        dataHeaderText = dataHeaderText.replace(DATA_TABLE_NAME, model.getName());

        List<FieldModel> fields = model.getDatatypeModel().getFields();
        int width = CollectionUtils.isEmpty(fields) ? 0 : fields.size() - 1;
        addMergedHeader(sheet, startPosition, headerStyle, new CellRangeSettings(headerSettings.getHeight(), width));

        Cell topLeftCell = PoiExcelHelper.getOrCreateCell(startPosition.getColumn(), startPosition.getRow(), sheet);
        RichTextString dtHeader = new XSSFRichTextString(dataHeaderText);
        int typeStart = dataHeaderText.indexOf(type);
        int typeEnd = typeStart + type.length();
        dtHeader.applyFont(typeStart, typeEnd, typeFont);
        dtHeader.applyFont(typeEnd + 1, dataHeaderText.length() - 1, tableNameFont);
        topLeftCell.setCellValue(dtHeader);
        startPosition = startPosition.moveDown(headerSettings.getHeight());

        Cursor endPosition = startPosition;

        if (CollectionUtils.isNotEmpty(fields)) {
            for (FieldModel fm : fields) {
                String fieldName = fm.getName();
                String formattedName = formatName(fieldName);

                Cursor next = endPosition.moveDown(1);
                Cell subheaderCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
                subheaderCell.setCellValue(fieldName);
                subheaderCell.setCellStyle(subheaderStyle);

                next = next.moveDown(1);
                Cell columnHeaderCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
                columnHeaderCell.setCellValue(formattedName);
                columnHeaderCell.setCellStyle(style.getColumnHeaderStyle());

                next = next.moveDown(1);
                Cell rowCell = PoiExcelHelper.getOrCreateCell(next.getColumn(), next.getRow(), sheet);
                writeDefaultValueToCell(model, fm, rowCell, dateStyle, dateTimeStyle);
                CellStyle styleAfterWrite = rowCell.getCellStyle();
                if (styleAfterWrite.getDataFormat() == 0) {
                    rowCell.setCellStyle(style.getRowStyle().getValueStyle());
                }

                endPosition = next.moveUp(ROWS_COUNT).moveRight(1);
            }
        }
        if (width > 0) {
            endPosition = endPosition.moveDown(ROWS_COUNT).moveLeft(width + 1);
        }
        return new Cursor(endPosition.getColumn(), endPosition.getRow());
    }

    private String formatName(String fieldName) {
        return StringUtils.join(Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(fieldName))
            .map(StringUtils::capitalize)
            .collect(Collectors.toList()), ' ');
    }

    @Override
    protected String getExcelSheetName() {
        return DATA_SHEET;
    }
}
