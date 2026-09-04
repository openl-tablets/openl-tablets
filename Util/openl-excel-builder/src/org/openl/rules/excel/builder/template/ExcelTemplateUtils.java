package org.openl.rules.excel.builder.template;

import static org.openl.rules.excel.builder.export.DataTableExporter.DATA_SHEET;
import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.EnvironmentTableExporter.ENV_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.DataTypeTableRowStyleImpl;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;
import org.openl.rules.excel.builder.template.row.NameValueRowStyleImpl;
import org.openl.rules.excel.builder.template.row.SpreadsheetTableRowStyleImpl;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.formatters.FormatConstants;

@Slf4j
public class ExcelTemplateUtils {

    public static final byte LEFT_MARGIN = 1;
    public static final byte TOP_MARGIN = 2;
    public static final String DATATYPE_DEFINITION = "{datatype.name}";
    public static final String DATA_TABLE_NAME = "{table.name}";
    public static final String DATA_TABLE_TYPE = "{returnType}";
    public static final short DATE_TIME_FORMAT = (short) BuiltinFormats.getBuiltinFormat("m/d/yy h:mm");
    public static final short DATE_FORMAT = (short) BuiltinFormats
            .getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT);

    private ExcelTemplateUtils() {
    }

    public static SXSSFWorkbook getTemplate() throws IOException {
        var classLoader = ExcelTemplateUtils.class.getClassLoader();
        var template = new XSSFWorkbook(classLoader.getResourceAsStream("template.xlsx"));
        var sheets = template.getNumberOfSheets();
        for (var i = 0; i < sheets; i++) {
            template.removeSheetAt(0);
        }
        return new SXSSFWorkbook(template);
    }

    public static Map<String, TableStyle> extractTemplateInfo(Workbook targetWorkbook) {
        var templateStyles = new HashMap<String, TableStyle>();
        var classLoader = ExcelTemplateUtils.class.getClassLoader();
        try (OPCPackage fs = OPCPackage
                .open(Objects.requireNonNull(classLoader.getResourceAsStream("template.xlsx"), "Template wasn't found."))) {
            var wb = new XSSFWorkbook(fs);

            var dataTypeSheet = wb.getSheet(DATATYPES_SHEET);
            if (dataTypeSheet == null) {
                log.error("Datatype sheet template wasn't found.");
            }

            TableStyle dataTypeStyle = extractDatatypeStyle(dataTypeSheet, targetWorkbook);
            templateStyles.put(DATATYPES_SHEET, dataTypeStyle);

            var sprResultSheet = wb.getSheet(SPR_RESULT_SHEET);
            if (sprResultSheet == null) {
                log.error("SpreadSheetResults sheet template wasn't found.");
            }

            TableStyle spreadSheetStyle = extractSpreadSheetResultStyle(sprResultSheet, targetWorkbook);
            templateStyles.put(SPR_RESULT_SHEET, spreadSheetStyle);

            var environmentSheet = wb.getSheet(ENV_SHEET);
            if (environmentSheet == null) {
                log.error("Environment sheet template wasn't found.");
            }
            TableStyle envStyle = extractEnvStyle(environmentSheet, targetWorkbook);
            templateStyles.put(ENV_SHEET, envStyle);

            var dataSheet = wb.getSheet(DATA_SHEET);
            if (dataSheet == null) {
                log.error("Data table template wasn't found.");
            }
            TableStyle dataStyle = extractDataTableStyle(dataSheet, targetWorkbook);
            templateStyles.put(DATA_SHEET, dataStyle);

        } catch (InvalidFormatException e) {
            log.error("Invalid format exception occurred.", e);
        } catch (IOException e) {
            log.error("There was a problem with reading the template file.", e);
        }

        return templateStyles;
    }

    private static TableStyle extractSpreadSheetResultStyle(Sheet sprResultSheet, Workbook targetWorkbook) {
        Cell sprResultHeader = extractTableHeader(sprResultSheet);
        CellStyle targetTableHeaderStyle = copyCellStyle(targetWorkbook, sprResultHeader);

        var sprTableHeaderText = sprResultHeader.getRichStringCellValue();

        var headerRegion = sprResultSheet.getMergedRegion(0);
        var headerSettings = new CellRangeSettings(headerRegion);

        var sprColumnHeaders = sprResultSheet.getRow(TOP_MARGIN + 1);
        var sprStepHeader = sprColumnHeaders.getCell(LEFT_MARGIN);
        var sprValueHeader = sprColumnHeaders.getCell(LEFT_MARGIN + 1);

        CellStyle targetStepHeaderStyle = copyCellStyle(targetWorkbook, sprStepHeader);

        CellStyle targetValueHeaderStyle = copyCellStyle(targetWorkbook, sprValueHeader);

        var stepHeader = sprStepHeader.getStringCellValue();
        var valueHeader = sprValueHeader.getStringCellValue();

        var headerRowStyle = new SpreadsheetTableRowStyleImpl(targetStepHeaderStyle,
                targetValueHeaderStyle);

        var sprFieldRow = sprResultSheet.getRow(TOP_MARGIN + 2);

        var sprFieldName = sprFieldRow.getCell(LEFT_MARGIN);
        CellStyle targetFieldStyle = copyCellStyle(targetWorkbook, sprFieldName);

        var sprFieldValue = sprFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetValueStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        var rowStyle = new SpreadsheetTableRowStyleImpl(targetFieldStyle, targetValueStyle);

        CellStyle dateStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        dateStyle.setDataFormat(DATE_FORMAT);

        CellStyle dateTimeStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        dateTimeStyle.setDataFormat(DATE_TIME_FORMAT);

        var lastSprRow = sprResultSheet.getRow(TOP_MARGIN + 3);

        var lastFieldName = lastSprRow.getCell(LEFT_MARGIN);
        CellStyle targetLastFieldStyle = copyCellStyle(targetWorkbook, lastFieldName);

        var lastFieldValue = lastSprRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetLastValueStyle = copyCellStyle(targetWorkbook, lastFieldValue);
        var lastRowStyle = new SpreadsheetTableRowStyleImpl(targetLastFieldStyle, targetLastValueStyle);

        return new SpreadsheetTableStyleImpl(sprTableHeaderText,
                targetTableHeaderStyle,
                headerSettings,
                headerRowStyle,
                stepHeader,
                valueHeader,
                rowStyle,
                lastRowStyle,
                dateStyle,
                dateTimeStyle);
    }

    private static TableStyle extractDatatypeStyle(Sheet dataTypeSheet, Workbook targetWorkbook) {
        Cell datatypeHeaderCell = extractTableHeader(dataTypeSheet);

        var headerRegion = dataTypeSheet.getMergedRegion(0);
        var headerSettings = new CellRangeSettings(headerRegion);

        CellStyle targetHeaderStyle = copyCellStyle(targetWorkbook, datatypeHeaderCell);

        var headerValueString = datatypeHeaderCell.getRichStringCellValue();
        var headerText = headerValueString.getString();
        var start = headerText.indexOf(DATATYPE_DEFINITION);
        var datatypeFont = ((XSSFRichTextString) headerValueString).getFontAtIndex(start);

        Font targetFont = copyFont(targetWorkbook, datatypeFont);

        var datatypeFieldRow = dataTypeSheet.getRow(TOP_MARGIN + 1);

        var dtFieldClass = datatypeFieldRow.getCell(LEFT_MARGIN);
        CellStyle targetClassStyle = copyCellStyle(targetWorkbook, dtFieldClass);

        var dtFieldName = datatypeFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetNameStyle = copyCellStyle(targetWorkbook, dtFieldName);

        var datatypeDefaultValueCell = datatypeFieldRow.getCell(LEFT_MARGIN + 2);
        var dvStyle = datatypeDefaultValueCell.getCellStyle();
        CellStyle targetDefaultValueStyle = copyStyle(targetWorkbook, dvStyle);
        CellStyle dateStyle = copyStyle(targetWorkbook, dvStyle);
        dateStyle.setDataFormat(DATE_FORMAT);

        CellStyle dateTimeStyle = copyStyle(targetWorkbook, dvStyle);
        dateTimeStyle.setDataFormat(DATE_TIME_FORMAT);

        var rowStyle = new DataTypeTableRowStyleImpl(targetClassStyle,
                targetNameStyle,
                targetDefaultValueStyle);

        var lastDataTypeRow = dataTypeSheet.getRow(TOP_MARGIN + 2);

        var dtLastFieldClassStyle = lastDataTypeRow.getCell(LEFT_MARGIN);
        CellStyle targetLastClassStyle = copyCellStyle(targetWorkbook, dtLastFieldClassStyle);

        var dtLastFieldNameStyle = lastDataTypeRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetLastFieldNameStyle = copyCellStyle(targetWorkbook, dtLastFieldNameStyle);

        var dtLastDefaultValueCell = lastDataTypeRow.getCell(LEFT_MARGIN + 2);
        CellStyle targetLastDefaultValueStyle = copyCellStyle(targetWorkbook, dtLastDefaultValueCell);
        var lastRowStyle = new DataTypeTableRowStyleImpl(targetLastClassStyle,
                targetLastFieldNameStyle,
                targetLastDefaultValueStyle);

        return new DataTypeTableStyleImpl(headerValueString,
                targetHeaderStyle,
                headerSettings,
                rowStyle,
                dateStyle,
                dateTimeStyle,
                lastRowStyle,
                targetFont);

    }

    private static Font copyFont(Workbook targetWorkbook, Font sourceFont) {
        var targetFont = targetWorkbook.createFont();
        targetFont.setBold(sourceFont.getBold());
        targetFont.setFontHeight(sourceFont.getFontHeight());
        targetFont.setColor(sourceFont.getColor());
        targetFont.setFontName(sourceFont.getFontName());
        targetFont.setItalic(sourceFont.getItalic());
        return targetFont;
    }

    private static TableStyle extractEnvStyle(Sheet envSheet, Workbook targetWorkbook) {
        Cell envHeaderCell = extractTableHeader(envSheet);
        CellStyle targetTableHeaderStyle = copyCellStyle(targetWorkbook, envHeaderCell);

        var envHeaderText = envHeaderCell.getRichStringCellValue();

        var headerRegion = envSheet.getMergedRegion(0);
        var headerSettings = new CellRangeSettings(headerRegion);

        var regularRow = envSheet.getRow(TOP_MARGIN + 1);

        NameValueRowStyle regularRowStyle = extractRowStyle(targetWorkbook, regularRow);

        var lastRow = envSheet.getRow(TOP_MARGIN + 2);

        NameValueRowStyle lastRowStyle = extractRowStyle(targetWorkbook, lastRow);

        return new EnvironmentTableStyleImpl(envHeaderText,
                targetTableHeaderStyle,
                headerSettings,
                regularRowStyle,
                lastRowStyle);
    }

    private static TableStyle extractDataTableStyle(Sheet dataSheet, Workbook targetWorkbook) {
        Cell dataTableHeader = extractTableHeader(dataSheet);
        CellStyle targetHeaderStyle = copyCellStyle(targetWorkbook, dataTableHeader);

        var headerRegion = dataSheet.getMergedRegion(0);
        var headerSettings = new CellRangeSettings(headerRegion);

        var headerText = (XSSFRichTextString) dataTableHeader.getRichStringCellValue();
        var headerTextString = headerText.getString();
        var sourceTypeFont = headerText.getFontAtIndex(headerTextString.indexOf(DATA_TABLE_TYPE));
        var sourceTableNameFont = headerText.getFontAtIndex(headerTextString.indexOf(DATA_TABLE_NAME));

        Font typeFont = copyFont(targetWorkbook, sourceTypeFont);
        Font tableNameFont = copyFont(targetWorkbook, sourceTableNameFont);

        var subheaderRow = dataSheet.getRow(TOP_MARGIN + 1);
        var subheaderCell = subheaderRow.getCell(LEFT_MARGIN);
        CellStyle targetSubheaderStyle = copyCellStyle(targetWorkbook, subheaderCell);

        var columnHeaderRow = dataSheet.getRow(TOP_MARGIN + 2);
        var columnHeaderCell = columnHeaderRow.getCell(LEFT_MARGIN);
        CellStyle columnHeaderStyle = copyCellStyle(targetWorkbook, columnHeaderCell);

        var valueRow = dataSheet.getRow(TOP_MARGIN + 3);
        var valueCell = valueRow.getCell(LEFT_MARGIN);
        CellStyle valueCellStyle = copyCellStyle(targetWorkbook, valueCell);

        CellStyle dateFieldStyle = copyStyle(targetWorkbook, valueCellStyle);
        dateFieldStyle.setDataFormat(DATE_FORMAT);
        CellStyle dateTimeFieldStyle = copyStyle(targetWorkbook, valueCellStyle);
        dateFieldStyle.setDataFormat(DATE_TIME_FORMAT);

        return new DataTableStyleImpl(headerText,
                targetHeaderStyle,
                headerSettings,
                typeFont,
                tableNameFont,
                targetSubheaderStyle,
                columnHeaderStyle,
                new NameValueRowStyleImpl(valueCellStyle, valueCellStyle),
                dateFieldStyle,
                dateTimeFieldStyle);
    }

    private static NameValueRowStyle extractRowStyle(Workbook targetWorkbook, Row regularRow) {
        var regularNameCell = regularRow.getCell(LEFT_MARGIN);
        CellStyle targetNameStyle = copyCellStyle(targetWorkbook, regularNameCell);

        var regularValueCell = regularRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetValueStyle = copyCellStyle(targetWorkbook, regularValueCell);

        return new NameValueRowStyleImpl(targetNameStyle, targetValueStyle);
    }

    private static CellStyle copyCellStyle(Workbook targetWorkbook, Cell sourceCell) {
        var classStyle = sourceCell.getCellStyle();
        return copyStyle(targetWorkbook, classStyle);
    }

    private static Cell extractTableHeader(Sheet dataTypeSheet) {
        var datatypeHeaderRow = dataTypeSheet.getRow(TOP_MARGIN);
        return datatypeHeaderRow.getCell(LEFT_MARGIN);
    }

    private static CellStyle copyStyle(Workbook targetWorkbook, CellStyle style) {
        CellStyle targetFieldStyle = PoiExcelHelper.createCellStyle(targetWorkbook);
        targetFieldStyle.cloneStyleFrom(style);
        return targetFieldStyle;
    }
}
