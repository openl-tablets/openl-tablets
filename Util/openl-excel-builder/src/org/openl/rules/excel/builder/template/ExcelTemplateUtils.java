package org.openl.rules.excel.builder.template;

import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.EnvironmentTableExporter.ENV_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.DataTypeRowStyle;
import org.openl.rules.excel.builder.template.row.DataTypeTableRowStyleImpl;
import org.openl.rules.excel.builder.template.row.NameValueRowStyle;
import org.openl.rules.excel.builder.template.row.NameValueRowStyleImpl;
import org.openl.rules.excel.builder.template.row.SpreadsheetTableRowStyleImpl;
import org.openl.rules.table.xls.PoiExcelHelper;
import org.openl.rules.table.xls.formatters.FormatConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelTemplateUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExcelTemplateUtils.class);

    public static final byte LEFT_MARGIN = 1;
    public static final byte TOP_MARGIN = 2;
    public static final String DATATYPE_DEFINITON = "{datatype.name}";
    public static final short DATE_TIME_FORMAT = (short) BuiltinFormats.getBuiltinFormat("m/d/yy h:mm");
    public static final short DATE_FORMAT = (short) BuiltinFormats
        .getBuiltinFormat(FormatConstants.DEFAULT_XLS_DATE_FORMAT);

    private ExcelTemplateUtils() {
    }

    public static Map<String, TableStyle> extractTemplateInfo(Workbook targetWorkbook) {
        Map<String, TableStyle> templateStyles = new HashMap<>();
        ClassLoader classLoader = ExcelTemplateUtils.class.getClassLoader();
        try (OPCPackage fs = OPCPackage
            .open(Objects.requireNonNull(classLoader.getResourceAsStream("template.xlsx"), "Template wasn't found."))) {
            XSSFWorkbook wb = new XSSFWorkbook(fs);

            Sheet dataTypeSheet = wb.getSheet(DATATYPES_SHEET);
            if (dataTypeSheet == null) {
                logger.error("Datatype sheet template wasn't found.");
            }

            TableStyle dataTypeStyle = extractDatatypeStyle(dataTypeSheet, targetWorkbook);
            templateStyles.put(DATATYPES_SHEET, dataTypeStyle);

            Sheet sprResultSheet = wb.getSheet(SPR_RESULT_SHEET);
            if (sprResultSheet == null) {
                logger.error("SpreadSheetResults sheet template wasn't found.");
            }

            TableStyle spreadSheetStyle = extractSpreadSheetResultStyle(sprResultSheet, targetWorkbook);
            templateStyles.put(SPR_RESULT_SHEET, spreadSheetStyle);

            Sheet environmentSheet = wb.getSheet(ENV_SHEET);
            if (environmentSheet == null) {
                logger.error("Environment sheet template wasn't found.");
            }
            TableStyle envStyle = extractEnvStyle(environmentSheet, targetWorkbook);
            templateStyles.put(ENV_SHEET, envStyle);
        } catch (InvalidFormatException e) {
            logger.error("Invalid format exception occurred.", e);
        } catch (IOException e) {
            logger.error("There was a problem with reading the template file.", e);
        }

        return templateStyles;
    }

    private static TableStyle extractSpreadSheetResultStyle(Sheet sprResultSheet, Workbook targetWorkbook) {
        Cell sprResultHeader = extractTableHeader(sprResultSheet);
        CellStyle targetTableHeaderStyle = copyCellStyle(targetWorkbook, sprResultHeader);

        RichTextString sprTableHeaderText = sprResultHeader.getRichStringCellValue();

        CellRangeAddress headerRegion = sprResultSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        Row sprColumnHeaders = sprResultSheet.getRow(TOP_MARGIN + 1);
        Cell sprStepHeader = sprColumnHeaders.getCell(LEFT_MARGIN);
        Cell sprValueHeader = sprColumnHeaders.getCell(LEFT_MARGIN + 1);

        CellStyle targetStepHeaderStyle = copyCellStyle(targetWorkbook, sprStepHeader);

        CellStyle targetValueHeaderStyle = copyCellStyle(targetWorkbook, sprValueHeader);

        String stepHeader = sprStepHeader.getStringCellValue();
        String valueHeader = sprValueHeader.getStringCellValue();

        NameValueRowStyle headerRowStyle = new SpreadsheetTableRowStyleImpl(targetStepHeaderStyle,
            targetValueHeaderStyle);

        Row sprFieldRow = sprResultSheet.getRow(TOP_MARGIN + 2);

        Cell sprFieldName = sprFieldRow.getCell(LEFT_MARGIN);
        CellStyle targetFieldStyle = copyCellStyle(targetWorkbook, sprFieldName);

        Cell sprFieldValue = sprFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetValueStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        NameValueRowStyle rowStyle = new SpreadsheetTableRowStyleImpl(targetFieldStyle, targetValueStyle);

        CellStyle dateStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        dateStyle.setDataFormat(DATE_FORMAT);

        CellStyle dateTimeStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        dateTimeStyle.setDataFormat(DATE_TIME_FORMAT);

        Row lastSprRow = sprResultSheet.getRow(TOP_MARGIN + 3);

        Cell lastFieldName = lastSprRow.getCell(LEFT_MARGIN);
        CellStyle targetLastFieldStyle = copyCellStyle(targetWorkbook, lastFieldName);

        Cell lastFieldValue = lastSprRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetLastValueStyle = copyCellStyle(targetWorkbook, lastFieldValue);
        NameValueRowStyle lastRowStyle = new SpreadsheetTableRowStyleImpl(targetLastFieldStyle, targetLastValueStyle);

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

        CellRangeAddress headerRegion = dataTypeSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        CellStyle targetHeaderStyle = copyCellStyle(targetWorkbook, datatypeHeaderCell);

        RichTextString headerValueString = datatypeHeaderCell.getRichStringCellValue();
        String headerText = headerValueString.getString();
        int start = headerText.indexOf(DATATYPE_DEFINITON);
        XSSFFont datatypeFont = ((XSSFRichTextString) headerValueString).getFontAtIndex(start);

        Font targetFont = targetWorkbook.createFont();
        targetFont.setBold(datatypeFont.getBold());
        targetFont.setFontHeight(datatypeFont.getFontHeight());
        targetFont.setColor(datatypeFont.getColor());
        targetFont.setFontName(datatypeFont.getFontName());
        targetFont.setItalic(datatypeFont.getItalic());

        Row datatypeFieldRow = dataTypeSheet.getRow(TOP_MARGIN + 1);

        Cell dtFieldClass = datatypeFieldRow.getCell(LEFT_MARGIN);
        CellStyle targetClassStyle = copyCellStyle(targetWorkbook, dtFieldClass);

        String datatypeFieldValueTemplate = dtFieldClass.getStringCellValue();

        Cell dtFieldName = datatypeFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetNameStyle = copyCellStyle(targetWorkbook, dtFieldName);

        String datatypeNameTemplate = dtFieldName.getStringCellValue();

        Cell datatypeDefaultValueCell = datatypeFieldRow.getCell(LEFT_MARGIN + 2);
        CellStyle dvStyle = datatypeDefaultValueCell.getCellStyle();
        CellStyle targetDefaultValueStyle = copyStyle(targetWorkbook, dvStyle);
        CellStyle dateStyle = copyStyle(targetWorkbook, dvStyle);
        dateStyle.setDataFormat(DATE_FORMAT);

        CellStyle dateTimeStyle = copyStyle(targetWorkbook, dvStyle);
        dateTimeStyle.setDataFormat(DATE_TIME_FORMAT);

        String datatypeDefaultTemplate = datatypeDefaultValueCell.getStringCellValue();

        DataTypeRowStyle rowStyle = new DataTypeTableRowStyleImpl(targetClassStyle,
            targetNameStyle,
            targetDefaultValueStyle);

        Row lastDataTypeRow = dataTypeSheet.getRow(TOP_MARGIN + 2);

        Cell dtLastFieldClassStyle = lastDataTypeRow.getCell(LEFT_MARGIN);
        CellStyle targetLastClassStyle = copyCellStyle(targetWorkbook, dtLastFieldClassStyle);

        Cell dtLastFieldNameStyle = lastDataTypeRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetLastFieldNameStyle = copyCellStyle(targetWorkbook, dtLastFieldNameStyle);

        Cell dtLastDefaultValueCell = lastDataTypeRow.getCell(LEFT_MARGIN + 2);
        CellStyle targetLastDefaultValueStyle = copyCellStyle(targetWorkbook, dtLastDefaultValueCell);
        DataTypeRowStyle lastRowStyle = new DataTypeTableRowStyleImpl(targetLastClassStyle,
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

    private static TableStyle extractEnvStyle(Sheet envSheet, Workbook targetWorkbook) {
        Cell envHeaderCell = extractTableHeader(envSheet);
        CellStyle targetTableHeaderStyle = copyCellStyle(targetWorkbook, envHeaderCell);

        RichTextString envHeaderText = envHeaderCell.getRichStringCellValue();

        CellRangeAddress headerRegion = envSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        Row regularRow = envSheet.getRow(TOP_MARGIN + 1);

        NameValueRowStyle regularRowStyle = extractRowStyle(targetWorkbook, regularRow);

        Row lastRow = envSheet.getRow(TOP_MARGIN + 2);

        NameValueRowStyle lastRowStyle = extractRowStyle(targetWorkbook, lastRow);

        return new EnvironmentTableStyleImpl(envHeaderText,
            targetTableHeaderStyle,
            headerSettings,
            regularRowStyle,
            lastRowStyle);
    }

    private static NameValueRowStyle extractRowStyle(Workbook targetWorkbook, Row regularRow) {
        Cell regularNameCell = regularRow.getCell(LEFT_MARGIN);
        CellStyle targetNameStyle = copyCellStyle(targetWorkbook, regularNameCell);

        Cell regularValueCell = regularRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetValueStyle = copyCellStyle(targetWorkbook, regularValueCell);

        return new NameValueRowStyleImpl(targetNameStyle, targetValueStyle);
    }

    private static CellStyle copyCellStyle(Workbook targetWorkbook, Cell sourceCell) {
        CellStyle classStyle = sourceCell.getCellStyle();
        return copyStyle(targetWorkbook, classStyle);
    }

    private static Cell extractTableHeader(Sheet dataTypeSheet) {
        Row datatypeHeaderRow = dataTypeSheet.getRow(TOP_MARGIN);
        return datatypeHeaderRow.getCell(LEFT_MARGIN);
    }

    private static CellStyle copyStyle(Workbook targetWorkbook, CellStyle style) {
        CellStyle targetFieldStyle = PoiExcelHelper.createCellStyle(targetWorkbook);
        targetFieldStyle.cloneStyleFrom(style);
        return targetFieldStyle;
    }
}
