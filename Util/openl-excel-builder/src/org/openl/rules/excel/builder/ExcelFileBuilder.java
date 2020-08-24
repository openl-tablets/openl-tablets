package org.openl.rules.excel.builder;

import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.xssf.streaming.CustomizedSXSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.openl.rules.excel.builder.export.DatatypeTableExporter;
import org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter;
import org.openl.rules.excel.builder.template.ExcelTemplateUtils;
import org.openl.rules.excel.builder.template.TableStyle;
import org.openl.rules.model.scaffolding.DatatypeModel;
import org.openl.rules.model.scaffolding.ProjectModel;
import org.openl.rules.model.scaffolding.SpreadsheetResultModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build the xlsx datatype spreadsheet from the given data type list
 */
public class ExcelFileBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ExcelFileBuilder.class);

    private ExcelFileBuilder() {
    }

    /**
     * Generating xlsx file with project which contains data types and spreadsheets.
     * 
     * @param projectModel - model of the project.
     */
    public static void generateProject(ProjectModel projectModel) {
        List<SpreadsheetResultModel> sprs = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> dts = projectModel.getDatatypeModels();
        String projectName = projectModel.getName();
        String fileName = projectName + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            writeProject(sprs, dts, fos);
        } catch (IOException e) {
            logger.error("Error on saving the file occurred.", e);
        }
    }

    /**
     * Generate project model to the the output stream.
     * 
     * @param projectModel - model of the project with data types and spreadsheets.
     * @param outputStream - output stream, which contains result file.
     */
    public static void generateProject(ProjectModel projectModel, OutputStream outputStream) {
        writeProject(projectModel.getSpreadsheetResultModels(), projectModel.getDatatypeModels(), outputStream);
    }

    /**
     * Generate data type to the output stream.
     * 
     * @param datatypeModels - data type models.
     * @param outputStream - output stream with models.
     */
    public static void generateDataTypes(List<DatatypeModel> datatypeModels, OutputStream outputStream) {
        writeDataTypes(datatypeModels, outputStream);
    }

    /**
     * Generate spreadsheets to the output stream.
     * 
     * @param spreadsheetResultModels - spreadsheet models.
     * @param outputStream - output stream with models.
     */
    public static void generateSpreadsheets(List<SpreadsheetResultModel> spreadsheetResultModels,
            OutputStream outputStream) {
        writeSpreadsheets(spreadsheetResultModels, outputStream);
    }

    /**
     * Writing models to Excel file with styles from template.
     * 
     * @param datatypeModels
     * @param outputStream
     */
    private static void writeDataTypes(List<DatatypeModel> datatypeModels, OutputStream outputStream) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = new CustomizedSXSSFWorkbook()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            SXSSFSheet dtSheet = workbook.createSheet(DATATYPES_SHEET);
            TableStyle datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            DatatypeTableExporter datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);
            datatypeTableExporter.export(datatypeModels, dtSheet);
            dtSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            logger.error("Error on generating DataTypes workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    /**
     * Writing spreadsheets to Excel file with styles from template.
     * 
     * @param spreadsheetResultModels
     * @param outputStream
     */
    private static void writeSpreadsheets(List<SpreadsheetResultModel> spreadsheetResultModels,
            OutputStream outputStream) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = new CustomizedSXSSFWorkbook()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);
            SXSSFSheet sprSheet = workbook.createSheet(SPR_RESULT_SHEET);
            TableStyle sprStyles = stylesMap.get(SPR_RESULT_SHEET);
            List<String> sprNames = spreadsheetResultModels.stream()
                .map(SpreadsheetResultModel::getName)
                .collect(Collectors.toList());
            SpreadsheetResultTableExporter sprTableExporter = new SpreadsheetResultTableExporter(sprNames);
            sprTableExporter.setTableStyle(sprStyles);
            sprTableExporter.export(spreadsheetResultModels, sprSheet);
            sprSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(outputStream);
        } catch (IOException e) {
            logger.error("Error on generating Spreadsheet workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    /**
     * Writing project to Excel file with styles from template.
     * 
     * @param sprs - spreadsheets.
     * @param dts - data types.
     * @param fos - output stream.
     */
    private static void writeProject(List<SpreadsheetResultModel> sprs, List<DatatypeModel> dts, OutputStream fos) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = new CustomizedSXSSFWorkbook()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);

            SXSSFSheet dtSheet = workbook.createSheet(DATATYPES_SHEET);
            SXSSFSheet sprSheet = workbook.createSheet(SPR_RESULT_SHEET);

            TableStyle datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            TableStyle sprStyles = stylesMap.get(SPR_RESULT_SHEET);

            DatatypeTableExporter datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);

            List<String> sprNames = sprs.stream().map(SpreadsheetResultModel::getName).collect(Collectors.toList());
            SpreadsheetResultTableExporter sprTableExporter = new SpreadsheetResultTableExporter(sprNames);
            sprTableExporter.setTableStyle(sprStyles);

            datatypeTableExporter.export(dts, dtSheet);
            sprTableExporter.export(sprs, sprSheet);
            dtSheet.validateMergedRegions();
            sprSheet.validateMergedRegions();
            autoSizeSheets(workbook);
            workbook.write(fos);
        } catch (IOException e) {
            logger.error("Error on generating workbook occurred.", e);
        } finally {
            if (tempWorkbook != null) {
                tempWorkbook.dispose();
            }
        }
    }

    /**
     * Make sheets readable.
     * 
     * @param workbook - target document.
     */
    private static void autoSizeSheets(SXSSFWorkbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            sheet.trackAllColumnsForAutoSizing();
            autoSizeColumns(sheet);
        }
    }

    /**
     * Normalize the width of the column.
     * 
     * @param sheet
     */
    private static void autoSizeColumns(SXSSFSheet sheet) {
        SXSSFRow row = sheet.getRow(sheet.getLastRowNum());
        if (row == null) {
            return;
        }
        short lastColumn = row.getLastCellNum();
        for (int i = 1; i < lastColumn; i++) {
            sheet.autoSizeColumn(i, true);
        }
    }

}
