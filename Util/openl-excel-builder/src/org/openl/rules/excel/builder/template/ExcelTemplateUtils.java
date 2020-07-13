package org.openl.rules.excel.builder.template;

import static org.openl.rules.excel.builder.ExcelFileBuilder.VOCABULARY_SHEET;
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
import org.openl.rules.table.xls.XlsCellStyle;
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

            TableStyle dataTypeStyle = extractDatatypeStyle(wb, dataTypeSheet, targetWorkbook);
            templateStyles.put(DATATYPES_SHEET, dataTypeStyle);

            Sheet sprResultSheet = wb.getSheet(SPR_RESULT_SHEET);
            if (sprResultSheet == null) {
                logger.error("SpreadSheetResults sheet wasn't found.");
            }

            TableStyle spreadSheetStyle = extractSpreadSheetResultStyle(sprResultSheet, targetWorkbook);
            templateStyles.put(SPR_RESULT_SHEET, spreadSheetStyle);

            Sheet vocabularySheet = wb.getSheet(VOCABULARY_SHEET);
            if (vocabularySheet == null) {
                logger.error("Vocabulary sheet wasn't found.");
            }
            TableStyle vocabularyStyle = extractVocabularyStyle(vocabularySheet, targetWorkbook);
            templateStyles.put(VOCABULARY_SHEET, vocabularyStyle);

        } catch (InvalidFormatException e) {
            logger.error("Invalid format exception occurred.", e);
        } catch (IOException e) {
            logger.error("There was a problem with reading the template file.", e);
        }

        return templateStyles;
    }

    private static TableStyle extractVocabularyStyle(Sheet vocabularySheet, Workbook targetWorkbook) {
        Cell datatypeAliasHeader = extractTableHeader(vocabularySheet);
        CellStyle aliasHeaderStyle = datatypeAliasHeader.getCellStyle();
        CellStyle targetAliasHeaderStyle = copyStyle(targetWorkbook, aliasHeaderStyle);
        String aliasHeaderTemplate = datatypeAliasHeader.getStringCellValue();

        Row vocabularyValueRow = vocabularySheet.getRow(TOP_MARGIN + 1);
        Cell vocabularyCell = vocabularyValueRow.getCell(LEFT_MARGIN);
        CellStyle cellStyle = vocabularyCell.getCellStyle();
        return new DataTypeAliasTableStyle(aliasHeaderTemplate,
            createXlsCellStyle(targetWorkbook, targetAliasHeaderStyle),
            null,
            cellStyle);
    }

    private static TableStyle extractSpreadSheetResultStyle(Sheet sprResultSheet, Workbook targetWorkbook) {
        Cell sprResultHeader = extractTableHeader(sprResultSheet);
        CellStyle sprResultStyle = sprResultHeader.getCellStyle();
        CellStyle targetTableHeaderStyle = copyStyle(targetWorkbook, sprResultStyle);

        String sprTableHeaderText = sprResultHeader.getStringCellValue();

        CellRangeAddress headerRegion = sprResultSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        Row sprColumnHeaders = sprResultSheet.getRow(TOP_MARGIN + 1);
        Cell sprStepHeader = sprColumnHeaders.getCell(LEFT_MARGIN);
        Cell sprValueHeader = sprColumnHeaders.getCell(LEFT_MARGIN + 1);

        CellStyle sprStepHeaderStyle = sprStepHeader.getCellStyle();
        CellStyle targetStepHeaderStyle = copyStyle(targetWorkbook, sprStepHeaderStyle);

        CellStyle sprValueHeaderStyle = sprValueHeader.getCellStyle();
        CellStyle targetValueHeaderStyle = copyStyle(targetWorkbook, sprValueHeaderStyle);

        String stepHeader = sprStepHeader.getStringCellValue();
        String valueHeader = sprValueHeader.getStringCellValue();

        Row sprFieldRow = sprResultSheet.getRow(TOP_MARGIN + 2);

        Cell sprFieldName = sprFieldRow.getCell(LEFT_MARGIN);
        CellStyle sprFieldStyle = sprFieldName.getCellStyle();
        CellStyle targetFieldStyle = copyStyle(targetWorkbook, sprFieldStyle);

        Cell sprFieldValue = sprFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle sprValueStyle = sprFieldValue.getCellStyle();
        CellStyle targetValueStyle = copyStyle(targetWorkbook, sprValueStyle);
        return new SpreadsheetResultTableStyle(sprTableHeaderText,
            createXlsCellStyle(targetWorkbook, targetTableHeaderStyle),
            headerSettings,
            createXlsCellStyle(targetWorkbook, targetStepHeaderStyle),
            createXlsCellStyle(targetWorkbook, targetValueHeaderStyle),
            stepHeader,
            valueHeader,
            createXlsCellStyle(targetWorkbook, targetFieldStyle),
            createXlsCellStyle(targetWorkbook, targetValueStyle));
    }

    private static XlsCellStyle createXlsCellStyle(Workbook targetWorkbook, CellStyle targetTableHeaderStyle) {
        return new XlsCellStyle(targetTableHeaderStyle, targetWorkbook);
    }

    private static CellStyle copyStyle(Workbook targetWorkbook, CellStyle sprFieldStyle) {
        CellStyle targetFieldStyle = targetWorkbook.createCellStyle();
        targetFieldStyle.cloneStyleFrom(sprFieldStyle);
        return targetFieldStyle;
    }

    private static TableStyle extractDatatypeStyle(Workbook sourceWb, Sheet dataTypeSheet, Workbook targetWorkbook) {
        Cell datatypeHeaderCell = extractTableHeader(dataTypeSheet);

        CellRangeAddress headerRegion = dataTypeSheet.getMergedRegion(0);
        CellRangeSettings headerSettings = new CellRangeSettings(headerRegion);

        CellStyle dtHeaderStyle = datatypeHeaderCell.getCellStyle();
        CellStyle targetHeaderStyle = copyStyle(targetWorkbook, dtHeaderStyle);

        String dtHeaderValue = datatypeHeaderCell.getStringCellValue();

        Row datatypeFieldRow = dataTypeSheet.getRow(TOP_MARGIN + 1);

        Cell dtFieldClass = datatypeFieldRow.getCell(LEFT_MARGIN);
        CellStyle classStyle = dtFieldClass.getCellStyle();
        CellStyle targetClassStyle = copyStyle(targetWorkbook, classStyle);

        String datatypeFieldValueTemplate = dtFieldClass.getStringCellValue();

        Cell dtFieldName = datatypeFieldRow.getCell(LEFT_MARGIN + 1);
        CellStyle nameStyle = dtFieldName.getCellStyle();
        CellStyle targetNameStyle = copyStyle(targetWorkbook, nameStyle);

        String datatypeNameTemplate = dtFieldName.getStringCellValue();

        Cell datatypeDefaultValueCell = datatypeFieldRow.getCell(LEFT_MARGIN + 2);
        CellStyle dvStyle = datatypeDefaultValueCell.getCellStyle();
        CellStyle targetDefaultValueStyle = copyStyle(targetWorkbook, dvStyle);

        String datatypeDefaultTemplate = datatypeDefaultValueCell.getStringCellValue();

        return new DataTypeTableStyle(dtHeaderValue,
            createXlsCellStyle(targetWorkbook, targetHeaderStyle),
            headerSettings,
            createXlsCellStyle(targetWorkbook, targetClassStyle),
            createXlsCellStyle(targetWorkbook, targetNameStyle),
            createXlsCellStyle(targetWorkbook, targetDefaultValueStyle));

    }

    private static Cell extractTableHeader(Sheet dataTypeSheet) {
        Row datatypeHeaderRow = dataTypeSheet.getRow(TOP_MARGIN);
        return datatypeHeaderRow.getCell(LEFT_MARGIN);
    }
}
