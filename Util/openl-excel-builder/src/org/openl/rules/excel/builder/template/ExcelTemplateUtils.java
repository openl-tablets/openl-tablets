package org.openl.rules.excel.builder.template;

import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.excel.builder.CellRangeSettings;
import org.openl.rules.excel.builder.template.row.DataTypeTableRowStyle;
import org.openl.rules.excel.builder.template.row.SpreadsheetTableRowStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelTemplateUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExcelTemplateUtils.class);

    public static final byte LEFT_MARGIN = 1;
    public static final byte TOP_MARGIN = 2;

    private ExcelTemplateUtils() {
    }

    public static Map<String, TableStyle> extractTemplateInfo(Workbook targetWorkbook) {
        Map<String, TableStyle> templateStyles = new HashMap<>();
        ClassLoader classLoader = ExcelTemplateUtils.class.getClassLoader();
        try (OPCPackage fs = OPCPackage
            .open(Objects.requireNonNull(classLoader.getResourceAsStream("template.xlsx")))) {
            XSSFWorkbook wb = new XSSFWorkbook(fs);

            Sheet dataTypeSheet = wb.getSheet(DATATYPES_SHEET);
            if (dataTypeSheet == null) {
                logger.info("Datatype sheet wasn't found.");
            }

            TableStyle dataTypeStyle = extractDatatypeStyle(dataTypeSheet, targetWorkbook);
            templateStyles.put(DATATYPES_SHEET, dataTypeStyle);

            Sheet sprResultSheet = wb.getSheet(SPR_RESULT_SHEET);
            if (sprResultSheet == null) {
                logger.error("SpreadSheetResults sheet wasn't found.");
            }

            TableStyle spreadSheetStyle = extractSpreadSheetResultStyle(sprResultSheet, targetWorkbook);
            templateStyles.put(SPR_RESULT_SHEET, spreadSheetStyle);

        } catch (InvalidFormatException e) {
            logger.error("Invalid format exception occurred.", e);
        } catch (IOException e) {
            logger.error("There was a problem with reading the template file.", e);
        }

        return templateStyles;
    }

    private static TableStyle extractVocabularyStyle(Sheet vocabularySheet, Workbook targetWorkbook) {
        Cell datatypeAliasHeader = extractTableHeader(vocabularySheet);
        CellStyle targetAliasHeaderStyle = copyCellStyle(targetWorkbook, datatypeAliasHeader);
        String aliasHeaderTemplate = datatypeAliasHeader.getStringCellValue();

        Row vocabularyValueRow = vocabularySheet.getRow(TOP_MARGIN + 1);
        Cell vocabularyCell = vocabularyValueRow.getCell(LEFT_MARGIN);
        CellStyle cellStyle = vocabularyCell.getCellStyle();
        return new DataTypeAliasTableStyle(aliasHeaderTemplate, targetAliasHeaderStyle, null, cellStyle);
    }

    private static TableStyle extractSpreadSheetResultStyle(Sheet sprResultSheet, Workbook targetWorkbook) {
        Cell sprResultHeader = extractTableHeader(sprResultSheet);
        CellStyle targetTableHeaderStyle = copyCellStyle(targetWorkbook, sprResultHeader);

        String sprTableHeaderText = sprResultHeader.getStringCellValue();

        CellRangeAddress headerRegion = sprResultSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        Row sprColumnHeaders = sprResultSheet.getRow(TOP_MARGIN + 1);
        Cell sprStepHeader = sprColumnHeaders.getCell(LEFT_MARGIN);
        Cell sprValueHeader = sprColumnHeaders.getCell(LEFT_MARGIN + 1);

        CellStyle targetStepHeaderStyle = copyCellStyle(targetWorkbook, sprStepHeader);

        CellStyle targetValueHeaderStyle = copyCellStyle(targetWorkbook, sprValueHeader);

        String stepHeader = sprStepHeader.getStringCellValue();
        String valueHeader = sprValueHeader.getStringCellValue();

        SpreadsheetTableRowStyle headerRowStyle = new SpreadsheetTableRowStyle(targetStepHeaderStyle,
            targetValueHeaderStyle);

        Row sprFieldRow = sprResultSheet.getRow(TOP_MARGIN + 2);

        Cell sprFieldName = sprFieldRow.getCell(LEFT_MARGIN);
        CellStyle targetFieldStyle = copyCellStyle(targetWorkbook, sprFieldName);

        Cell sprFieldValue = sprFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetValueStyle = copyCellStyle(targetWorkbook, sprFieldValue);
        SpreadsheetTableRowStyle rowStyle = new SpreadsheetTableRowStyle(targetFieldStyle, targetValueStyle);

        Row lastSprRow = sprResultSheet.getRow(TOP_MARGIN + 3);

        Cell lastFieldName = lastSprRow.getCell(LEFT_MARGIN);
        CellStyle targetLastFieldStyle = copyCellStyle(targetWorkbook, lastFieldName);

        Cell lastFieldValue = lastSprRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetLastValueStyle = copyCellStyle(targetWorkbook, lastFieldValue);
        SpreadsheetTableRowStyle lastRowStyle = new SpreadsheetTableRowStyle(targetLastFieldStyle,
            targetLastValueStyle);

        return new SpreadsheetResultTableStyle(sprTableHeaderText,
            targetTableHeaderStyle,
            headerSettings,
            headerRowStyle,
            stepHeader,
            valueHeader,
            rowStyle,
            lastRowStyle);
    }

    private static TableStyle extractDatatypeStyle(Sheet dataTypeSheet, Workbook targetWorkbook) {
        Cell datatypeHeaderCell = extractTableHeader(dataTypeSheet);

        CellRangeAddress headerRegion = dataTypeSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        CellStyle targetHeaderStyle = copyCellStyle(targetWorkbook, datatypeHeaderCell);

        String dtHeaderValue = datatypeHeaderCell.getStringCellValue();

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

        String datatypeDefaultTemplate = datatypeDefaultValueCell.getStringCellValue();

        DataTypeTableRowStyle rowStyle = new DataTypeTableRowStyle(targetClassStyle,
            targetNameStyle,
            targetDefaultValueStyle);

        Row lastDataTypeRow = dataTypeSheet.getRow(TOP_MARGIN + 2);

        Cell dtLastFieldClassStyle = lastDataTypeRow.getCell(LEFT_MARGIN);
        CellStyle targetLastClassStyle = copyCellStyle(targetWorkbook, dtLastFieldClassStyle);

        Cell dtLastFieldNameStyle = lastDataTypeRow.getCell(LEFT_MARGIN + 1);
        CellStyle targetLastFieldNameStyle = copyCellStyle(targetWorkbook, dtLastFieldNameStyle);

        Cell dtLastDefaultValueCell = lastDataTypeRow.getCell(LEFT_MARGIN + 2);
        CellStyle targetLastDefaultValueStyle = copyCellStyle(targetWorkbook, dtLastDefaultValueCell);
        DataTypeTableRowStyle lastRowStyle = new DataTypeTableRowStyle(targetLastClassStyle,
            targetLastFieldNameStyle,
            targetLastDefaultValueStyle);

        return new DataTypeTableStyle(dtHeaderValue,
            targetHeaderStyle,
            headerSettings,
            rowStyle,
            dateStyle,
            lastRowStyle);

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
        CellStyle targetFieldStyle = targetWorkbook.createCellStyle();
        targetFieldStyle.cloneStyleFrom(style);
        return targetFieldStyle;
    }
}
