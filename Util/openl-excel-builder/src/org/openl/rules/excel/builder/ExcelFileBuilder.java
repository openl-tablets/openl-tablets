package org.openl.rules.excel.builder;

import static org.openl.rules.excel.builder.export.DatatypeTableExporter.DATATYPES_SHEET;
import static org.openl.rules.excel.builder.export.SpreadsheetResultTableExporter.SPR_RESULT_SHEET;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
     * 
     * @param projectModel - model of the project
     */
    public static void generateExcelFile(ProjectModel projectModel) {
        List<SpreadsheetResultModel> sprs = projectModel.getSpreadsheetResultModels();
        List<DatatypeModel> dts = projectModel.getDatatypeModels();
        String projectName = projectModel.getName();
        String fileName = projectName + ".xlsx";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            makeWorkbook(sprs, dts, fos);
        } catch (IOException e) {
            logger.error("Error on saving the file occurred.", e);
        }

    }

    private static void makeWorkbook(List<SpreadsheetResultModel> sprs, List<DatatypeModel> dts, FileOutputStream fos) {
        SXSSFWorkbook tempWorkbook = null;
        try (SXSSFWorkbook workbook = tempWorkbook = new CustomizedSXSSFWorkbook()) {
            Map<String, TableStyle> stylesMap = ExcelTemplateUtils.extractTemplateInfo(workbook);

            SXSSFSheet dtSheet = workbook.createSheet(DATATYPES_SHEET);
            SXSSFSheet sprSheet = workbook.createSheet(SPR_RESULT_SHEET);

            TableStyle datatypeStyles = stylesMap.get(DATATYPES_SHEET);
            TableStyle sprStyles = stylesMap.get(SPR_RESULT_SHEET);

            DatatypeTableExporter datatypeTableExporter = new DatatypeTableExporter();
            datatypeTableExporter.setTableStyle(datatypeStyles);

            SpreadsheetResultTableExporter sprTableExporter = new SpreadsheetResultTableExporter();
            sprTableExporter.setTableStyle(sprStyles);
            // possible bottleneck
            dtSheet.validateMergedRegions();
            sprSheet.validateMergedRegions();

            datatypeTableExporter.export(dts, dtSheet);
            sprTableExporter.export(sprs, sprSheet);
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

    private static void autoSizeSheets(SXSSFWorkbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            sheet.trackAllColumnsForAutoSizing();
            autoSizeColumns(sheet);
        }
    }

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
